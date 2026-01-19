package com.example.aiautominitest.ui.chat

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiautominitest.core.IChatEngine
import com.example.aiautominitest.data.model.ChatMessage
import com.example.aiautominitest.data.repo.ChatRepository
import com.example.aiautominitest.data.repo.IModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(
    private val chatEngine: IChatEngine,
    private val chatRepository: ChatRepository,
    private val modelRepository: IModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    
    init {
        // Load messages from database
        viewModelScope.launch {
            chatRepository.getAllMessages().collect { dbMessages ->
                _messages.value = dbMessages
                if (_uiState.value is ChatUiState.ModelReady || _uiState.value is ChatUiState.Chatting) {
                    updateChattingState(dbMessages, false, "")
                }
            }
        }
        
        // Auto-initialize model from default path
        checkAndInitModel()
    }

    private fun checkAndInitModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ChatUiState.ModelLoading
            
            // Check if model is already downloaded
            val modelPath = modelRepository.getModelPath()
            if (modelPath != null && File(modelPath).exists()) {
                initializeModel(File(modelPath).parent ?: "")
            } else {
                // Try default path for MiniCPM
                val defaultPath = File(Environment.getExternalStorageDirectory(), "MiniCPM")
                if (defaultPath.exists()) {
                    initializeModel(defaultPath.absolutePath)
                } else {
                    _uiState.value = ChatUiState.ModelError(
                        "模型文件未找到。请将模型放置在 /sdcard/MiniCPM/ 目录下"
                    )
                }
            }
        }
    }

    private fun initializeModel(modelDirPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = chatEngine.init(modelDirPath, "")
                if (success) {
                    _uiState.value = ChatUiState.ModelReady(modelDirPath)
                    // Transition to chatting state
                    updateChattingState(_messages.value, false, "")
                } else {
                    _uiState.value = ChatUiState.ModelError("模型初始化失败")
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.ModelError("模型初始化异常: ${e.message}")
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            // Save user message
            val userMessage = ChatMessage(
                role = ChatMessage.ROLE_USER,
                content = content,
                status = 1
            )
            val messageId = chatRepository.insertMessage(userMessage)
            
            // Update UI to show generating
            updateChattingState(_messages.value, true, "")
            
            // Get AI response
            try {
                val history = _messages.value.takeLast(10) // Last 10 messages for context
                var fullResponse = ""
                
                chatEngine.chat(history, content)
                    .flowOn(Dispatchers.IO)
                    .collect { token ->
                        fullResponse += token
                        // Update UI with streaming response
                        updateChattingState(_messages.value, true, fullResponse)
                    }
                
                // Save AI response
                val aiMessage = ChatMessage(
                    role = ChatMessage.ROLE_ASSISTANT,
                    content = fullResponse,
                    status = 1
                )
                chatRepository.insertMessage(aiMessage)
                
                // Update UI - generation complete
                updateChattingState(_messages.value, false, "")
                
            } catch (e: Exception) {
                // Handle error
                val errorMessage = ChatMessage(
                    role = ChatMessage.ROLE_ASSISTANT,
                    content = "错误: ${e.message}",
                    status = 2
                )
                chatRepository.insertMessage(errorMessage)
                updateChattingState(_messages.value, false, "")
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _messages.value = emptyList()
            updateChattingState(emptyList(), false, "")
        }
    }

    private fun updateChattingState(
        messages: List<ChatMessage>,
        isGenerating: Boolean,
        currentResponse: String
    ) {
        _uiState.value = ChatUiState.Chatting(
            messages = messages,
            isGenerating = isGenerating,
            currentResponse = currentResponse
        )
    }

    override fun onCleared() {
        super.onCleared()
        chatEngine.release()
    }
}
