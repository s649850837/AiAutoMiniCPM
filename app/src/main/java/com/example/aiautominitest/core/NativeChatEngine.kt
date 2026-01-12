package com.example.aiautominitest.core

import com.example.aiautominitest.data.model.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NativeChatEngine : IChatEngine {
    
    private val bridge = MNNLLMBridge()

    override fun init(modelPath: String, tokenizerPath: String): Boolean {
        return bridge.init(modelPath) 
    }

    override fun chat(history: List<ChatMessage>, input: String): Flow<String> = callbackFlow {
        bridge.onTokenCallback = { token ->
            trySend(token)
        }
        
        // Simple history conversion, can be improved to match specific model template
        val historyArray = history.map { "${it.role}: ${it.content}" }.toTypedArray()
        
        // Run in background thread if not already
        // But JNI call might be blocking, so usually run in IO dispatcher
        val success = bridge.chat(historyArray, input)
        if (!success) {
            // close(Exception("Inference failed"))
        }
        
        close()
        
        awaitClose { 
            bridge.stop()
            bridge.onTokenCallback = null
        }
    }

    override fun stop() {
        bridge.stop()
    }

    override fun release() {
        // cleanup
    }
}
