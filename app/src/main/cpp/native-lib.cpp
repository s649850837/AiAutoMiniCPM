#include <android/log.h>
#include <iostream>
#include <jni.h>
#include <memory>
#include <string>
#include <vector>

// MNN Headers
// Ensure these files exist in app/src/main/cpp/include/llm/
#include <llm/llm.hpp>

#define TAG "MNN_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<MNN::Transformer::Llm> g_llm;
static JavaVM *g_jvm = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  g_jvm = vm;
  return JNI_VERSION_1_6;
}

// Callback stream buffer to redirect std::ostream to Kotlin callback
class CallbackBuffer : public std::streambuf {
public:
  CallbackBuffer(std::function<void(const char *)> cb) : cb_(cb) {}

protected:
  std::streamsize xsputn(const char *s, std::streamsize n) override {
    buffer_.append(s, n);
    processBuffer();
    return n;
  }

  int overflow(int c) override {
    if (c != EOF) {
      buffer_.push_back(static_cast<char>(c));
      processBuffer();
    }
    return c;
  }

private:
  std::function<void(const char *)> cb_;
  std::string buffer_;

  void processBuffer() {
    size_t processed = 0;
    size_t len = buffer_.length();

    while (processed < len) {
      unsigned char c = static_cast<unsigned char>(buffer_[processed]);
      int seqLen = 0;

      if ((c & 0x80) == 0) {
        seqLen = 1; // ASCII
      } else if ((c & 0xE0) == 0xC0) {
        seqLen = 2; // 2-byte sequence
      } else if ((c & 0xF0) == 0xE0) {
        seqLen = 3; // 3-byte sequence
      } else if ((c & 0xF8) == 0xF0) {
        seqLen = 4; // 4-byte sequence
      } else {
        // Invalid start byte (e.g., continuation byte 0x80-0xBF without start)
        // Or invalid UTF-8. Treat as 1 byte to discard or pass through (likely
        // garbage). NewStringUTF might still complain, but we can't buffer it
        // forever.
        seqLen = 1;
      }

      if (processed + seqLen > len) {
        // Incomplete sequence, wait for more data
        break;
      }
      processed += seqLen;
    }

    if (processed > 0) {
      std::string validChunk = buffer_.substr(0, processed);
      cb_(validChunk.c_str());
      buffer_.erase(0, processed);
    }
  }
};

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_aiautominitest_core_MNNLLMBridge_init(JNIEnv *env,
                                                       jobject thiz,
                                                       jstring modelPath) {

  const char *path = env->GetStringUTFChars(modelPath, nullptr);
  std::string pathStr(path);
  env->ReleaseStringUTFChars(modelPath, path);

  LOGD("Initializing MNN LLM from: %s", pathStr.c_str());

  std::string configPath = pathStr + "/config.json";

  // Factory create
  // Note: Depends on MNN version. Assuming createLLM(config_path) works.
  auto *llm = MNN::Transformer::Llm::createLLM(configPath.c_str());

  if (llm) {
    g_llm.reset(llm);
    LOGD("MNN LLM created successfully");
    try {
      g_llm->load();
      LOGD("MNN LLM loaded weights");
      return JNI_TRUE;
    } catch (const std::exception &e) {
      LOGE("Exception during load: %s", e.what());
    }
  } else {
    LOGE("Failed to create MNN LLM instance");
  }

  return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_aiautominitest_core_MNNLLMBridge_chat(JNIEnv *env,
                                                       jobject thiz,
                                                       jobjectArray history,
                                                       jstring input) {

  if (!g_llm) {
    LOGE("LLM not initialized");
    return JNI_FALSE;
  }

  const char *inputC = env->GetStringUTFChars(input, nullptr);
  std::string inputStr(inputC);
  env->ReleaseStringUTFChars(input, inputC);

  // Get callback method ID
  jclass clazz = env->GetObjectClass(thiz);
  jmethodID onTokenMethod =
      env->GetMethodID(clazz, "onTokenGenerated", "(Ljava/lang/String;)V");
  if (!onTokenMethod) {
    LOGE("Could not find onTokenGenerated method");
    return JNI_FALSE;
  }

  // Prepare thread-safe callback
  // Note: 'thiz' is a local ref. If we used it in a separate thread we'd need a
  // GlobalRef. Here we are in the same native thread, but 'response' uses
  // current thread. However, if we wanted to be super safe or if 'response'
  // spins threads that call back? MNN 'response' usually writes to ostream on
  // the calling thread.

  auto callback = [&](const char *token) {
    // JNIEnv is thread specific. If response runs on same thread, we can use
    // 'env'. If MNN internally uses threads to write to ostream (unlikely for
    // ostream), we might need GetEnv. Assuming synchronous write for now.

    jstring jToken = env->NewStringUTF(token);
    env->CallVoidMethod(thiz, onTokenMethod, jToken);
    env->DeleteLocalRef(jToken);
  };

  CallbackBuffer buf(callback);
  std::ostream os(&buf);

  // Run inference
  // Ignoring history array for now, assuming stateful LLM.
  // TODO: Handle context reset/history if needed.
  g_llm->response(inputStr, &os);

  return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_aiautominitest_core_MNNLLMBridge_stop(JNIEnv *env,
                                                       jobject thiz) {
  if (g_llm) {
    // g_llm->stop(); // Not standard in all MNN versions
    LOGD("Stop requested - not fully implemented for synchronous bridge");
  }
}
