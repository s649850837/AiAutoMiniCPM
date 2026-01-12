package com.example.aiautominitest.core

import com.example.aiautominitest.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface IChatEngine {
    fun init(modelPath: String, tokenizerPath: String): Boolean
    fun chat(history: List<ChatMessage>, input: String): Flow<String>
    fun stop()
    fun release()
}
