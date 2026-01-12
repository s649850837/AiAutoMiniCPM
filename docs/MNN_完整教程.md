# MNN LLM Android 集成完整教程

> 📘 **适用对象**：Android 开发初学者和对 AI 模型部署感兴趣的开发者  
> 🎯 **目标**：从零开始，在 Android 应用中集成 MNN 推理引擎，实现本地 AI 对话功能

---

## 📚 目录

1. [MNN 基础知识](#1-mnn-基础知识)
2. [为什么选择 MNN](#2-为什么选择-mnn)
3. [技术架构说明](#3-技术架构说明)
4. [准备工作](#4-准备工作)
5. [第一步：构建 MNN 库](#5-第一步构建-mnn-库)
6. [第二步：集成到 Android](#6-第二步集成到-android)
7. [第三步：JNI 层实现](#7-第三步jni-层实现)
8. [第四步：Kotlin 层封装](#8-第四步kotlin-层封装)
9. [第五步：使用示例](#9-第五步使用示例)
10. [常见问题与解决](#10-常见问题与解决)
11. [性能优化建议](#11-性能优化建议)

---

## 1. MNN 基础知识

### 1.1 什么是 MNN？

**MNN**（Mobile Neural Network）是阿里巴巴开源的轻量级深度学习推理引擎，专为移动端和嵌入式设备优化。

**简单理解：**
- 🧠 **AI 的"翻译官"**：将训练好的 AI 模型（如 ChatGPT、GPT-4）转换为手机能理解的格式
- 📱 **手机上的 AI 引擎**：让你的 Android 应用能在**本地**运行大语言模型（LLM），无需联网
- ⚡ **性能优化专家**：针对 ARM 处理器进行深度优化，比直接运行 PyTorch 模型快得多

### 1.2 核心概念解释

| 术语 | 简单解释 | 举例 |
|------|----------|------|
| **推理引擎** | 运行 AI 模型的"解释器" | 就像 Java 需要 JVM，MNN 是 AI 模型的运行环境 |
| **LLM** | Large Language Model（大语言模型） | ChatGPT、MiniCPM 等能对话的 AI |
| **JNI** | Java Native Interface（Java 原生接口） | Kotlin/Java 和 C++ 代码之间的"桥梁" |
| **ARM82** | ARM 处理器的指令集扩展 | 支持 FP16（半精度浮点运算），加速 AI 计算 |
| **Token** | 文本的最小单位 | 中文通常 1 个字 = 1-2 个 token |

---

## 2. 为什么选择 MNN

### 2.1 MNN vs 其他方案

```mermaid
graph LR
    A[AI 模型部署方案] --> B[云端 API]
    A --> C[TensorFlow Lite]
    A --> D[ONNX Runtime]
    A --> E[MNN]
    
    B --> B1[优点: 无需本地资源]
    B --> B2[缺点: 需要网络、隐私风险]
    
    C --> C1[优点: Google 官方支持]
    C --> C2[缺点: LLM 支持有限]
    
    D --> D1[优点: 跨平台通用]
    D2[缺点: 移动端优化不足]
    
    E --> E1[优点: 专为移动优化]
    E --> E2[优点: LLM 原生支持]
    E --> E3[优点: 国内社区活跃]
</mermaid>

### 2.2 MNN 的优势

✅ **离线运行**：用户数据不上传，保护隐私  
✅ **低延迟**：本地推理，响应速度快（无网络延迟）  
✅ **成本低**：无需支付云端 API 费用  
✅ **专项优化**：针对 ARM 芯片优化（Snapdragon、Dimensity）  
✅ **功能完整**：支持流式输出（像 ChatGPT 那样逐字显示）

---

## 3. 技术架构说明

### 3.1 整体架构图

```mermaid
graph TB
    subgraph Android 应用层
        A[MainActivity.kt]
        B[ChatViewModel.kt]
        C[NativeChatEngine.kt]
    end
    
    subgraph JNI 桥接层
        D[MNNLLMBridge.kt]
        E[native-lib.cpp]
    end
    
    subgraph MNN 引擎层
        F[libMNN.so<br/>128 MB]
        G[MNN::Transformer::Llm]
    end
    
    subgraph 模型文件
        H[config.json]
        I[model.mnn]
        J[tokenizer.json]
    end
    
    A --> B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G --> H
    G --> I
    G --> J
    
    style F fill:#ff6b6b
    style E fill:#4ecdc4
    style C fill:#95e1d3
</mermaid>

### 3.2 数据流程说明

**用户发送消息 → AI 回复**的完整流程：

```mermaid
sequenceDiagram
    participant User as 👤 用户
    participant UI as 📱 MainActivity
    participant VM as 🧩 ViewModel
    participant Engine as ⚙️ NativeChatEngine
    participant JNI as 🌉 JNI (C++)
    participant MNN as 🤖 MNN 引擎
    
    User->>UI: 输入 "你好"
    UI->>VM: 调用 sendMessage()
    VM->>Engine: chat(history, "你好")
    Engine->>JNI: bridge.chat()
    JNI->>MNN: llm->response()
    
    Note over MNN: 模型推理开始
    
    loop 流式输出
        MNN-->>JNI: 生成 token "你"
        JNI-->>Engine: onTokenGenerated("你")
        Engine-->>VM: Flow.emit("你")
        VM-->>UI: 更新 UI
        UI-->>User: 显示 "你"
    end
    
    Note over MNN: 推理结束
    MNN-->>JNI: 结束信号
    JNI-->>Engine: close()
</mermaid>

---

## 4. 准备工作

### 4.1 系统要求

| 项目 | 要求 |
|------|------|
| **操作系统** | Windows 10/11、macOS 或 Linux |
| **Android Studio** | 2022.1 或更高版本 |
| **NDK 版本** | 25.1+ (推荐 27.2) |
| **CMake** | 3.22.1 或更高版本 |
| **Gradle** | 8.2+ |
| **JDK** | 17 |
| **磁盘空间** | 至少 10 GB（MNN 源码 + 编译产物） |

### 4.2 安装必要工具

#### 4.2.1 安装 Android SDK 和 NDK

1. 打开 Android Studio → SDK Manager
2. 勾选以下项目：
   - ✅ Android SDK Platform (API 26+)
   - ✅ NDK (Side by side) - 选择 25.1.8937393 或更高
   - ✅ CMake

#### 4.2.2 验证 NDK 安装

```powershell
# Windows PowerShell
dir "D:\Sdk\ndk"
# 应该看到 25.1.8937393 或 27.2.12479018 文件夹

# 验证 CMake
cmake --version
# 输出：cmake version 3.22.1 或更高
```

#### 4.2.3 安装 Git

```powershell
# Windows
winget install Git.Git

# 验证
git --version
```

---

## 5. 第一步：构建 MNN 库

### 5.1 为什么要从源码构建？

MNN 官方**不提供**预编译的 Android LLM 库，原因：
- LLM 功能较新，预编译版本可能不包含
- 不同项目需要不同的编译选项（OpenCL、Vulkan、FP16 等）
- 源码构建可以针对特定芯片优化

### 5.2 克隆 MNN 源码

```powershell
# 进入你的工作目录
cd D:\AIWorkSpace\AiAutoMiniCPM

# 克隆 MNN 仓库（约 500 MB）
git clone https://github.com/alibaba/MNN.git MNN_Repo
cd MNN_Repo

# 查看版本（当前示例使用 3.3.1）
git log -1 --oneline
```

### 5.3 理解构建选项

| CMake 选项 | 作用 | 是否启用 |
|-----------|------|----------|
| `-DMNN_BUILD_LLM=ON` | 启用大语言模型支持 | ✅ 必须 |
| `-DMNN_SUPPORT_TRANSFORMER_FUSE=ON` | 启用 Transformer 融合优化 | ✅ 推荐 |
| `-DMNN_ARM82=ON` | 启用 ARMv8.2 FP16 加速 | ✅ 推荐 |
| `-DMNN_OPENCL=ON` | 启用 GPU 加速（OpenCL） | ⚠️ 可选 |
| `-DMNN_LOW_MEMORY=ON` | 低内存模式（适合 LLM） | ✅ 推荐 |
| `-DMNN_SEP_BUILD=OFF` | 生成单体库（而非多个 `.so`） | ✅ 简化部署 |

### 5.4 创建构建脚本

创建文件 `build_mnn_android.ps1`：

```powershell
# MNN Android 构建脚本
$CMAKE_EXE = "D:\Sdk\cmake\3.22.1\bin\cmake.exe"
$NINJA_EXE = "D:\Sdk\cmake\3.22.1\bin\ninja.exe"
$TOOLCHAIN = "D:\Sdk\ndk\25.1.8937393\build\cmake\android.toolchain.cmake"
$SOURCE = "D:\AIWorkSpace\AiAutoMiniCPM\MNN_Repo"
$BUILD = "$SOURCE\build_android_arm64"

Write-Host "🚀 开始构建 MNN Android 库..."

# 创建构建目录
New-Item -ItemType Directory -Path $BUILD -Force | Out-Null

# 配置 CMake
Write-Host "📝 配置 CMake..."
& $CMAKE_EXE -G "Ninja" -S $SOURCE -B $BUILD `
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" `
    -DCMAKE_MAKE_PROGRAM="$NINJA_EXE" `
    -DANDROID_ABI="arm64-v8a" `
    -DANDROID_PLATFORM=android-21 `
    -DANDROID_STL=c++_shared `
    -DMNN_BUILD_LLM=ON `
    -DMNN_SUPPORT_TRANSFORMER_FUSE=ON `
    -DMNN_ARM82=ON `
    -DMNN_LOW_MEMORY=ON `
    -DMNN_OPENCL=ON `
    -DMNN_BUILD_OPENCV=ON `
    -DMNN_IMGCODECS=ON `
    -DMNN_SEP_BUILD=OFF `
    -DCMAKE_BUILD_TYPE=Release

if ($LASTEXITCODE -eq 0) {
    Write-Host "🔨 开始编译（预计 5-10 分钟）..."
    & $CMAKE_EXE --build $BUILD --parallel 8
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 构建成功！"
        Write-Host "📦 生成的库位于: $BUILD\OFF\arm64-v8a\libMNN.so"
    }
}
```

### 5.5 执行构建

```powershell
# 运行脚本
powershell -ExecutionPolicy Bypass -File .\build_mnn_android.ps1

# ⏱️ 等待 5-10 分钟...
# 你会看到类似输出：
# [1/770] Building CXX object ...
# [2/770] Building CXX object ...
# ...
# [770/770] Linking CXX shared library libMNN.so
```

### 5.6 验证构建结果

```powershell
# 检查生成的库
ls MNN_Repo\build_android_arm64\OFF\arm64-v8a\libMNN.so

# 输出示例：
# -a----  2026/1/12  128880376 libMNN.so  ← 约 128 MB
```

> ⚠️ **常见错误：**
> - **错误 1**：`'android/log.h' file not found` → NDK 未正确安装
> - **错误 2**：`ninja: build stopped` → 内存不足，减少 `--parallel` 参数
> - **错误 3**：`OBJECT library cannot have POST_BUILD` → 需要修补 CMakeLists.txt（见[故障排除](#101-cmake-配置错误)）

---

## 6. 第二步：集成到 Android

### 6.1 项目目录结构

```
AiAutoMiniCPM/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/                    ← C++ 代码目录
│   │   │   │   ├── include/            ← 头文件
│   │   │   │   │   ├── MNN/
│   │   │   │   │   │   ├── Interpreter.hpp
│   │   │   │   │   │   ├── Tensor.hpp
│   │   │   │   │   │   └── expr/
│   │   │   │   │   │       └── Expr.hpp
│   │   │   │   │   └── llm/
│   │   │   │   │       └── llm.hpp     ← LLM 头文件
│   │   │   │   ├── CMakeLists.txt      ← CMake 配置
│   │   │   │   └── native-lib.cpp      ← JNI 实现
│   │   │   ├── jniLibs/                ← 原生库目录
│   │   │   │   └── arm64-v8a/
│   │   │   │       └── libMNN.so       ← MNN 库（128 MB）
│   │   │   └── java/.../core/
│   │   │       ├── MNNLLMBridge.kt     ← Kotlin 桥接
│   │   │       └── NativeChatEngine.kt ← Kotlin 引擎
│   └── build.gradle.kts
└── MNN_Repo/                            ← MNN 源码（构建后可删除）
```

### 6.2 复制库文件和头文件

使用 PowerShell 脚本自动复制：

```powershell
$AppRoot = "D:\AIWorkSpace\AiAutoMiniCPM\app\src\main"
$MNNRepo = "D:\AIWorkSpace\AiAutoMiniCPM\MNN_Repo"

# 1. 复制 libMNN.so
Write-Host "📦 复制 libMNN.so..."
$JniLibs = "$AppRoot\jniLibs\arm64-v8a"
New-Item -ItemType Directory -Force -Path $JniLibs | Out-Null
Copy-Item "$MNNRepo\build_android_arm64\OFF\arm64-v8a\libMNN.so" `
    -Destination $JniLibs -Force

# 2. 复制 MNN 核心头文件
Write-Host "📄 复制 MNN 头文件..."
$IncludeMNN = "$AppRoot\cpp\include\MNN"
New-Item -ItemType Directory -Force -Path $IncludeMNN | Out-Null
Copy-Item "$MNNRepo\include\MNN\*" -Destination $IncludeMNN -Recurse -Force

# 3. 复制 LLM 头文件
Write-Host "📄 复制 LLM 头文件..."
$IncludeLLM = "$AppRoot\cpp\include\llm"
New-Item -ItemType Directory -Force -Path $IncludeLLM | Out-Null
Copy-Item "$MNNRepo\transformers\llm\engine\include\llm\*.hpp" `
    -Destination $IncludeLLM -Force

Write-Host "✅ 文件复制完成！"
```

### 6.3 配置 CMakeLists.txt

编辑 [app/src/main/cpp/CMakeLists.txt](file:///d:/AIWorkSpace/AiAutoMiniCPM/app/src/main/cpp/CMakeLists.txt)：

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("aiautominitest")

# 定义路径
set(LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(INCLUDE_DIR ${CMAKE_SOURCE_DIR}/include)

# 添加头文件搜索路径
include_directories(${INCLUDE_DIR})

# 导入 MNN 库（单体库，包含 LLM）
add_library(MNN SHARED IMPORTED)
set_target_properties(MNN PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/libMNN.so)

# 编译你的 JNI 代码
add_library(
        aiautominitest
        SHARED
        native-lib.cpp)

# 链接库
find_library(log-lib log)

target_link_libraries(
        aiautominitest
        MNN          # MNN 库
        ${log-lib})  # Android 日志
```

> 💡 **关键点解释：**
> - `SHARED IMPORTED`：告诉 CMake 这是一个预编译的动态库
> - `${ANDROID_ABI}`：自动匹配架构（arm64-v8a、armeabi-v7a 等）
> - `log-lib`：Android 系统日志库，用于调试

---

## 7. 第三步：JNI 层实现

### 7.1 JNI 基础概念

**JNI 的作用**：让 Kotlin/Java 代码能调用 C++ 函数。

```mermaid
graph LR
    A[Kotlin<br/>NativeChatEngine] -->|1. 调用| B[JNI 方法<br/>external fun init]
    B -->|2. 映射| C[C++ 函数<br/>Java_..._init]
    C -->|3. 执行| D[MNN C++ API<br/>Llm::createLLM]
    D -->|4. 回调| C
    C -->|5. 回调| B
    B -->|6. 返回| A
    
    style B fill:#4ecdc4
    style C fill:#ff6b6b
</mermaid>

### 7.2 编写 native-lib.cpp

完整代码见 [native-lib.cpp](file:///d:/AIWorkSpace/AiAutoMiniCPM/app/src/main/cpp/native-lib.cpp)：

```cpp
#include <android/log.h>
#include <jni.h>
#include <memory>
#include <string>
#include <llm/llm.hpp>  // MNN LLM 头文件

#define TAG "MNN_JNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 全局 LLM 实例
static std::unique_ptr<MNN::Transformer::Llm> g_llm;

// ========== 1. 初始化模型 ==========
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_aiautominitest_core_MNNLLMBridge_init(
        JNIEnv* env, jobject thiz, jstring modelPath) {
    
    // 1. 获取模型路径
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    std::string configPath = std::string(path) + "/config.json";
    env->ReleaseStringUTFChars(modelPath, path);
    
    // 2. 创建 LLM 实例
    auto* llm = MNN::Transformer::Llm::createLLM(configPath.c_str());
    if (!llm) {
        LOGE("❌ 创建 LLM 失败");
        return JNI_FALSE;
    }
    
    // 3. 加载模型权重
    g_llm.reset(llm);
    g_llm->load();
    
    return JNI_TRUE;
}

// ========== 2. 流式对话 ==========
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_aiautominitest_core_MNNLLMBridge_chat(
        JNIEnv* env, jobject thiz, jobjectArray history, jstring input) {
    
    if (!g_llm) {
        LOGE("❌ LLM 未初始化");
        return JNI_FALSE;
    }
    
    // 1. 获取用户输入
    const char* inputC = env->GetStringUTFChars(input, nullptr);
    std::string inputStr(inputC);
    env->ReleaseStringUTFChars(input, inputC);
    
    // 2. 获取回调方法 ID
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID onTokenMethod = env->GetMethodID(
        clazz, "onTokenGenerated", "(Ljava/lang/String;)V");
    
    // 3. 定义 C++ → Kotlin 回调
    auto callback = [&](const char* token) {
        jstring jToken = env->NewStringUTF(token);
        env->CallVoidMethod(thiz, onTokenMethod, jToken);
        env->DeleteLocalRef(jToken);
    };
    
    // 4. 创建自定义流缓冲区
    class CallbackBuffer : public std::streambuf {
    public:
        CallbackBuffer(std::function<void(const char*)> cb) : cb_(cb) {}
    protected:
        std::streamsize xsputn(const char* s, std::streamsize n) override {
            std::string str(s, n);
            cb_(str.c_str());
            return n;
        }
    private:
        std::function<void(const char*)> cb_;
    };
    
    CallbackBuffer buf(callback);
    std::ostream os(&buf);
    
    // 5. 调用 MNN 推理（流式输出）
    g_llm->response(inputStr, &os);
    
    return JNI_TRUE;
}
```

### 7.3 JNI 函数命名规则

**规则**：`Java_<包名>_<类名>_<方法名>`

示例：
```cpp
// Kotlin: com.example.aiautominitest.core.MNNLLMBridge.init()
// C++:    Java_com_example_aiautominitest_core_MNNLLMBridge_init()
```

**特殊字符替换**：
- `.` → `_`
- `_` → `_1`

---

## 8. 第四步：Kotlin 层封装

### 8.1 MNNLLMBridge.kt（JNI 桥接）

```kotlin
package com.example.aiautominitest.core

class MNNLLMBridge {
    companion object {
        init {
            System.loadLibrary("aiautominitest")  // 加载 JNI 库
        }
    }

    // 声明 JNI 方法
    external fun init(modelPath: String): Boolean
    external fun chat(history: Array<String>, input: String): Boolean
    external fun stop()
    
    // 回调方法（由 C++ 调用）
    fun onTokenGenerated(token: String) {
        onTokenCallback?.invoke(token)
    }
    
    var onTokenCallback: ((String) -> Unit)? = null
}
```

### 8.2 NativeChatEngine.kt（协程封装）

```kotlin
package com.example.aiautominitest.core

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NativeChatEngine : IChatEngine {
    private val bridge = MNNLLMBridge()

    override fun init(modelPath: String, tokenizerPath: String): Boolean {
        return bridge.init(modelPath)
    }

    override fun chat(
        history: List<ChatMessage>, 
        input: String
    ): Flow<String> = callbackFlow {
        // 1. 设置回调
        bridge.onTokenCallback = { token ->
            trySend(token)  // 发送到 Flow
        }
        
        // 2. 转换历史记录
        val historyArray = history.map { 
            "${it.role}: ${it.content}" 
        }.toTypedArray()
        
        // 3. 调用 JNI
        bridge.chat(historyArray, input)
        
        // 4. 清理
        awaitClose { 
            bridge.stop()
            bridge.onTokenCallback = null
        }
    }
}
```

---

## 9. 第五步：使用示例

### 9.1 在 ViewModel 中使用

```kotlin
class ChatViewModel : ViewModel() {
    private val chatEngine = NativeChatEngine()
    
    // UI 状态
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    init {
        // 初始化模型
        val modelPath = "/sdcard/MiniCPM"
        val success = chatEngine.init(modelPath, "")
        if (!success) {
            Log.e("ChatVM", "模型初始化失败")
        }
    }
    
    fun sendMessage(userInput: String) {
        viewModelScope.launch {
            // 1. 添加用户消息
            val userMsg = ChatMessage("user", userInput)
            _messages.value += userMsg
            
            // 2. 收集 AI 回复（流式）
            val aiResponse = StringBuilder()
            chatEngine.chat(_messages.value, userInput)
                .collect { token ->
                    aiResponse.append(token)
                    // 实时更新 UI
                    _messages.value = _messages.value.dropLast(1) + 
                        ChatMessage("assistant", aiResponse.toString())
                }
        }
    }
}
```

### 9.2 准备模型文件

#### 9.2.1 模型文件结构

```
/sdcard/MiniCPM/
├── config.json          ← 模型配置文件
├── embeddings_bf16.mnn  ← Embedding 层权重
├── lm.mnn               ← 主模型权重
└── tokenizer.json       ← 分词器
```

#### 9.2.2 config.json 示例

```json
{
  "llm_model": "lm.mnn",
  "llm_weight": "./",
  "tokenizer_file": "tokenizer.json",
  "backend_type": "cpu",
  "thread_num": 4,
  "prefill_thread_num": 8,
  "max_new_tokens": 512
}
```

### 9.3 权限配置

在 `AndroidManifest.xml` 中添加：

```xml
<manifest>
    <!-- 文件读取权限 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <application ...>
        <!-- 允许访问外部存储 -->
        android:requestLegacyExternalStorage="true"
    </application>
</manifest>
```

---

## 10. 常见问题与解决

### 10.1 CMake 配置错误

**错误信息：**
```
CMake Error: Target "llm" is an OBJECT library that may not have POST_BUILD commands.
```

**原因：** MNN 的 `CMakeLists.txt` 在 OBJECT 库上使用了 `add_custom_command`，不兼容新版 CMake。

**解决方案：**

编辑 `MNN_Repo/transformers/llm/engine/CMakeLists.txt`：

```cmake
# 找到这段代码（约第 68 行）
add_custom_command(
  TARGET llm
  POST_BUILD
  COMMAND ${CMAKE_COMMAND}
  ARGS -E copy_directory ${CMAKE_CURRENT_LIST_DIR}/include ${NATIVE_INCLUDE_OUTPUT}
)

# 注释掉
# add_custom_command(
#   TARGET llm
#   POST_BUILD
#   ...
# )
```

同样修改 `MNN_Repo/tools/cv/CMakeLists.txt`（约第 103 行）。

### 10.2 类名大小写错误

**错误信息：**
```cpp
error: no member named 'LLM' in namespace 'MNN::Transformer'
```

**原因：** 实际类名是 `Llm`（小写），不是 `LLM`。

**解决方案：**

修改 `native-lib.cpp`：
```cpp
// ❌ 错误
std::unique_ptr<MNN::Transformer::LLM> g_llm;

// ✅ 正确
std::unique_ptr<MNN::Transformer::Llm> g_llm;
```

### 10.3 运行时崩溃

**错误信息：**
```
java.lang.UnsatisfiedLinkError: dlopen failed: library "libMNN.so" not found
```

**原因：** `libMNN.so` 未正确打包到 APK。

**解决方案：**

1. 检查目录结构：
```bash
app/src/main/jniLibs/arm64-v8a/libMNN.so  # 必须在这个位置
```

2. 检查 `build.gradle.kts`：
```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += "arm64-v8a"  // 确保包含
        }
    }
}
```

3. 清理并重新构建：
```bash
./gradlew clean assembleDebug
```

### 10.4 模型加载失败

**错误信息：**
```
E/MNN_JNI: ❌ 创建 LLM 失败
```

**排查步骤：**

1. 检查文件路径：
```kotlin
val modelPath = "/sdcard/MiniCPM"
val configFile = File("$modelPath/config.json")
if (!configFile.exists()) {
    Log.e("TAG", "config.json 不存在！")
}
```

2. 检查权限：
```kotlin
if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) 
    != PERMISSION_GRANTED) {
    // 请求权限
}
```

3. 验证 `config.json` 格式：
```bash
adb shell cat /sdcard/MiniCPM/config.json
```

---

## 11. 性能优化建议

### 11.1 减少模型大小

**量化技术**：将 FP32 模型转为 INT4/INT8

```bash
# 使用 MNN 量化工具
./quantize_llm --model original.mnn --output quantized_int4.mnn --bits 4
```

| 量化方案 | 模型大小 | 推理速度 | 精度损失 |
|---------|---------|---------|---------|
| FP32 | 14 GB | 1x | 0% |
| FP16 | 7 GB | 1.5x | <1% |
| INT8 | 3.5 GB | 2x | 1-2% |
| INT4 | 1.8 GB | 3x | 2-5% |

### 11.2 启用 GPU 加速

修改 `config.json`：
```json
{
  "backend_type": "opencl",  // 从 "cpu" 改为 "opencl"
  "precision": "low"         // 使用 FP16 精度
}
```

### 11.3 调整线程数

根据设备 CPU 核心数调整：
```json
{
  "thread_num": 4,          // 推理线程（建议 = 大核数量）
  "prefill_thread_num": 8   // 预填充线程（建议 = 总核数）
}
```

---

## 12. 总结与下一步

### 12.1 你已经完成了什么

✅ 从 MNN 源码构建了 ARM64 推理引擎  
✅ 配置了完整的 JNI 桥接层  
✅ 实现了流式 AI 对话功能  
✅ 掌握了 Android 原生库集成流程

### 12.2 下一步建议

1. **获取模型**：下载或转换 MiniCPM 模型到 MNN 格式
2. **UI 优化**：实现打字机效果、Markdown 渲染
3. **功能扩展**：
   - 多轮对话历史管理
   - 模型热切换
   - 语音输入/输出
4. **性能调优**：
   - 使用 Profiler 分析性能瓶颈
   - 实现模型预加载
   - 添加缓存机制

### 12.3 参考资源

- 📖 MNN 官方文档：https://mnn-docs.readthedocs.io/
- 💬 MNN 社区论坛：https://github.com/alibaba/MNN/discussions
- 🎥 MNN LLM 示例项目：`MNN/apps/Android/MnnLlmChat`
- 📚 Android NDK 指南：https://developer.android.com/ndk/guides

---

**🎓 恭喜你！** 你现在已经掌握了在 Android 应用中集成 MNN 大语言模型的完整流程。有任何问题，随时查阅本文档的[常见问题](#10-常见问题与解决)部分。

**Happy Coding! 🚀**
