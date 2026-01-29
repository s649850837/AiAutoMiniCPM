package com.example.aiautominitest.ui.chat

import com.example.aiautominitest.data.model.ChatMessage

sealed class ChatUiState {
    object Idle : ChatUiState()
    object ModelLoading : ChatUiState()
    data class ModelCopying(
        val currentFile: String,
        val currentIndex: Int,
        val totalFiles: Int
    ) : ChatUiState() {
        val progress: Int
            get() = if (totalFiles > 0) (currentIndex * 100 / totalFiles) else 0
    }
    data class ModelReady(val modelPath: String) : ChatUiState()
    data class ModelError(val error: String) : ChatUiState()
    data class Chatting(
        val messages: List<ChatMessage>,
        val isGenerating: Boolean = false,
        val currentResponse: String = ""
    ) : ChatUiState()
}
