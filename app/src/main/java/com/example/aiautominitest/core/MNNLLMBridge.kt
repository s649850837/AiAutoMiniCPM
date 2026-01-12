package com.example.aiautominitest.core

class MNNLLMBridge {
    
    companion object {
        init {
            System.loadLibrary("aiautominitest")
        }
    }

    external fun init(modelPath: String): Boolean
    external fun chat(history: Array<String>, input: String): Boolean
    external fun stop()
    
    // Called from C++
    fun onTokenGenerated(token: String) {
        onTokenCallback?.invoke(token)
    }
    
    var onTokenCallback: ((String) -> Unit)? = null
}
