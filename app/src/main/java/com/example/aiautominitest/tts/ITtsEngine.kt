package com.example.aiautominitest.tts

import kotlinx.coroutines.flow.Flow

/**
 * TTS (Text-to-Speech) 引擎接口
 */
interface ITtsEngine {
    /**
     * 初始化 TTS 引擎
     * @param modelPath 模型目录路径
     * @return 是否初始化成功
     */
    suspend fun init(modelPath: String): Boolean

    /**
     * 合成语音
     * @param text 要合成的文本
     * @return 音频数据流 (PCM float samples)
     */
    fun synthesize(text: String): Flow<FloatArray>

    /**
     * 停止当前合成
     */
    fun stop()

    /**
     * 释放资源
     */
    fun release()

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean
}
