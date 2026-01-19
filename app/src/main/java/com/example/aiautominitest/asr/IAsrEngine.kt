package com.example.aiautominitest.asr

import kotlinx.coroutines.flow.Flow

/**
 * 语音识别引擎接口
 * 定义统一的ASR引擎API，支持多种实现
 */
interface IAsrEngine {
    /**
     * 初始化ASR引擎
     * @param modelPath ASR模型路径
     * @return 是否初始化成功
     */
    suspend fun init(modelPath: String): Boolean

    /**
     * 开始语音识别
     * @return Flow<AsrResult> 识别结果流
     */
    fun startRecognition(): Flow<AsrResult>

    /**
     * 停止语音识别
     */
    fun stopRecognition()

    /**
     * 重置识别器状态
     */
    fun reset()

    /**
     * 释放资源
     */
    fun release()

    /**
     * 检查引擎是否已初始化
     */
    fun isInitialized(): Boolean
}

/**
 * ASR识别结果
 */
sealed class AsrResult {
    /**
     * 部分识别结果（实时）
     * @param text 当前识别的文本
     * @param isEndpoint 是否检测到端点（说话结束）
     */
    data class Partial(
        val text: String,
        val isEndpoint: Boolean = false
    ) : AsrResult()

    /**
     * 最终识别结果
     * @param text 完整识别的文本
     */
    data class Final(val text: String) : AsrResult()

    /**
     * 识别错误
     * @param error 错误信息
     */
    data class Error(val error: String) : AsrResult()
}
