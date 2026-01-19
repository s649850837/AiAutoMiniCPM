package com.example.aiautominitest.ui.chat

import com.example.aiautominitest.data.model.ChatMessage

sealed class ChatUiState {
    object Idle : ChatUiState()
    object ModelLoading : ChatUiState()
    data class ModelReady(val modelPath: String) : ChatUiState()
    data class ModelError(val error: String) : ChatUiState()
    data class Chatting(
        val messages: List<ChatMessage>,
        val isGenerating: Boolean = false,
        val currentResponse: String = ""
    ) : ChatUiState()
}
