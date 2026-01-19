package com.example.aiautominitest.asr

import com.example.aiautominitest.audio.AudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Mock ASR 引擎（用于测试和演示）
 *
 * 这是一个临时实现，用于在 Sherpa-ONNX 集成之前测试 UI 功能。
 * 真实集成请使用 SherpaOnnxAsrEngine.
 *
 * 使用方法：
 * 在 ChatActivityWithVoice.kt 中:
 * private val asrEngine = MockAsrEngine()  // 使用 Mock 版本
 * // private val asrEngine = SherpaOnnxAsrEngine()  // 真实版本
 */
class MockAsrEngine : IAsrEngine {

    private var initialized = false
    private val audioRecorder = AudioRecorder()

    /**
     * 模拟初始化
     */
    override suspend fun init(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        // 模拟加载时间
        delay(1500)
        initialized = true
        true
    }

    /**
     * 模拟实时语音识别
     *
     * 实际流程：
     * 1. 录音 2 秒
     * 2. 模拟识别过程，逐步显示文本
     * 3. 检测端点
     */
    override fun startRecognition(): Flow<AsrResult> = flow {
        if (!initialized) {
            emit(AsrResult.Error("ASR engine not initialized"))
            return@flow
        }

        // 模拟识别流程
        val mockTexts = listOf(
            "你" to false,
            "你好" to false,
            "你好，" to false,
            "你好，我" to false,
            "你好，我是" to false,
            "你好，我是 AI" to false,
            "你好，我是 AI 助手" to true  // 端点检测
        )

        // 开始录音（实际采集音频）
        var sampleCount = 0
        audioRecorder.startRecording()
            .collect { samples ->
                sampleCount += samples.size

                // 模拟每 0.3 秒输出一次识别结果
                val textIndex = (sampleCount / (AudioRecorder.SAMPLE_RATE * 0.3)).toInt()

                if (textIndex < mockTexts.size) {
                    val (text, isEndpoint) = mockTexts[textIndex]
                    emit(AsrResult.Partial(text, isEndpoint))

                    if (isEndpoint) {
                        audioRecorder.stopRecording()
                        emit(AsrResult.Final(text))
                        return@collect
                    }
                }

                // 最多录音 5 秒
                if (sampleCount > AudioRecorder.SAMPLE_RATE * 5) {
                    audioRecorder.stopRecording()
                    if (mockTexts.isNotEmpty()) {
                        val finalText = mockTexts.last().first
                        emit(AsrResult.Final(finalText))
                    }
                }
            }
    }

    /**
     * 停止识别
     */
    override fun stopRecognition() {
        audioRecorder.stopRecording()
    }

    /**
     * 重置状态
     */
    override fun reset() {
        // Mock 实现无需重置
    }

    /**
     * 释放资源
     */
    override fun release() {
        audioRecorder.stopRecording()
        initialized = false
    }

    override fun isInitialized(): Boolean = initialized
}
