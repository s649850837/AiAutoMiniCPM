package com.example.aiautominitest.asr

import android.util.Log
import com.example.aiautominitest.audio.AudioRecorder
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sherpa-ONNX 语音识别引擎实现
 * 支持流式实时语音识别 (Streaming Transducer)
 */
class SherpaOnnxAsrEngine : IAsrEngine {
    companion object {
        private const val TAG = "SherpaOnnxAsrEngine"
    }

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private val audioRecorder = AudioRecorder()
    private var initialized = false

    /**
     * 初始化 Sherpa-ONNX 识别器
     */
    override suspend fun init(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (initialized) {
                Log.w(TAG, "Already initialized")
                return@withContext true
            }

            Log.i(TAG, "Initializing Sherpa-ONNX ASR with model: $modelPath")

            // 创建端点配置
            val endpointConfig = EndpointConfig(
                rule1 = EndpointRule(mustContainNonSilence = false, minTrailingSilence = 2.4f, minUtteranceLength = 0.0f),
                rule2 = EndpointRule(mustContainNonSilence = true, minTrailingSilence = 1.2f, minUtteranceLength = 0.0f),
                rule3 = EndpointRule(mustContainNonSilence = false, minTrailingSilence = 0.0f, minUtteranceLength = 20.0f)
            )

            // 创建特征配置
            val featConfig = FeatureConfig(
                sampleRate = AudioRecorder.SAMPLE_RATE,
                featureDim = 80
            )

            // 创建识别器配置
            val config = OnlineRecognizerConfig(
                featConfig = featConfig,
                modelConfig = getModelConfig(modelPath),
                endpointConfig = endpointConfig,
                enableEndpoint = true,
                decodingMethod = "greedy_search",
                maxActivePaths = 4
            )

            // 创建识别器
            recognizer = OnlineRecognizer(null, config)
            initialized = true

            Log.i(TAG, "Sherpa-ONNX ASR initialized successfully (Streaming)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ASR engine", e)
            false
        }
    }

    /**
     * 开始语音识别
     */
    override fun startRecognition(): Flow<AsrResult> = flow {
        if (!initialized || recognizer == null) {
            emit(AsrResult.Error("ASR engine not initialized"))
            return@flow
        }

        // 创建新的识别流
        stream = recognizer?.createStream()

        if (stream == null) {
            emit(AsrResult.Error("Failed to create recognition stream"))
            return@flow
        }

        Log.i(TAG, "Starting recognition (Streaming)")

        // 开始录音并处理音频数据
        audioRecorder.startRecording()
            .catch { e ->
                Log.e(TAG, "Audio recording error", e)
                emit(AsrResult.Error("Audio recording failed: ${e.message}"))
            }
            .collect { audioSamples ->
                stream?.let { s ->
                    // 将音频数据送入识别器
                    s.acceptWaveform(audioSamples, AudioRecorder.SAMPLE_RATE)

                    // 检查是否有识别结果
                    while (recognizer?.isReady(s) == true) {
                        recognizer?.decode(s)
                    }

                    // 获取当前识别结果
                    val result = recognizer?.getResult(s)
                    if (result != null && result.text.isNotEmpty()) {
                        // 检查是否检测到端点
                        val isEndpoint = recognizer?.isEndpoint(s) == true

                        emit(AsrResult.Partial(result.text, isEndpoint))

                        if (isEndpoint) {
                            // 重置流以开始新的识别
                            recognizer?.reset(s)
                            Log.d(TAG, "Endpoint detected, stream reset")
                        }
                    }
                }
            }
    }.flowOn(Dispatchers.IO)

    /**
     * 停止语音识别
     */
    override fun stopRecognition() {
        Log.i(TAG, "Stopping recognition")
        audioRecorder.stopRecording()

        // 获取最终结果
        stream?.let { s ->
            s.inputFinished()
            while (recognizer?.isReady(s) == true) {
                recognizer?.decode(s)
            }
        }

        stream = null
    }

    /**
     * 重置识别器状态
     */
    override fun reset() {
        stream?.let { recognizer?.reset(it) }
    }

    /**
     * 释放资源
     */
    override fun release() {
        Log.i(TAG, "Releasing ASR engine")
        stopRecognition()
        recognizer?.release()
        recognizer = null
        initialized = false
    }

    override fun isInitialized(): Boolean = initialized

    /**
     * 创建模型配置
     * 适配 sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20-mobile 文件名
     */
    private fun getModelConfig(modelPath: String): OnlineModelConfig {
        val tokens = "$modelPath/tokens.txt"
        
        // 尝试匹配 mobile 模型文件名 (int8 量化版本优先)
        val encoderInt8 = "$modelPath/encoder-epoch-99-avg-1.int8.onnx"
        val decoderInt8 = "$modelPath/decoder-epoch-99-avg-1.onnx" // decoder 通常较小，不一定有 int8
        val joinerInt8 = "$modelPath/joiner-epoch-99-avg-1.int8.onnx"

        // 尝试匹配标准 float 模型文件名
        val encoder = "$modelPath/encoder-epoch-99-avg-1.onnx"
        val decoder = "$modelPath/decoder-epoch-99-avg-1.onnx"
        val joiner = "$modelPath/joiner-epoch-99-avg-1.onnx"

        // 为了兼容旧逻辑，也检查 encoder.onnx 等
        val encoderSimple = "$modelPath/encoder.onnx"
        val decoderSimple = "$modelPath/decoder.onnx"
        val joinerSimple = "$modelPath/joiner.onnx"
        
        val encoderInt8Simple = "$modelPath/encoder.int8.onnx"
        val decoderInt8Simple = "$modelPath/decoder.int8.onnx"
        val joinerInt8Simple = "$modelPath/joiner.int8.onnx"


        return when {
            // Mobile Specific Filenames (Int8)
            File(encoderInt8).exists() -> {
                Log.i(TAG, "Loading Int8 Transducer Model (Mobile optimized)")
                OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = encoderInt8,
                        decoder = decoder, // decoder usually shares same name or has no int8 suffix
                        joiner = joinerInt8
                    ),
                    tokens = tokens,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            }
            // Mobile Specific Filenames (Float)
            File(encoder).exists() -> {
                Log.i(TAG, "Loading Float Transducer Model")
                OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = encoder,
                        decoder = decoder,
                        joiner = joiner
                    ),
                    tokens = tokens,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            }
             // Standard filenames (Simple)
            File(encoderSimple).exists() -> {
                 Log.i(TAG, "Loading Standard Transducer Model")
                 OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = encoderSimple,
                        decoder = decoderSimple,
                        joiner = joinerSimple
                    ),
                    tokens = tokens,
                    numThreads = 2,
                     debug = false,
                    provider = "cpu"
                )
            }
            // Standard filenames (Int8)
             File(encoderInt8Simple).exists() -> {
                 Log.i(TAG, "Loading Standard Int8 Transducer Model")
                 OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = encoderInt8Simple,
                        decoder = decoderInt8Simple,
                        joiner = joinerInt8Simple
                    ),
                    tokens = tokens,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            }
            else -> {
                throw IllegalArgumentException("No valid Transducer model files found in $modelPath")
            }
        }
    }
}
