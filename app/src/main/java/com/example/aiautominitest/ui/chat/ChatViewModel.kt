package com.example.aiautominitest.ui.chat

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiautominitest.core.IChatEngine
import com.example.aiautominitest.data.model.ChatMessage
import com.example.aiautominitest.data.repo.ChatRepository
import com.example.aiautominitest.data.repo.IModelRepository
// import com.example.aiautominitest.util.AssetModelCopier  // 已注释: 快速测试模式不使用Asset复制
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(
    private val context: Context,
    private val chatEngine: IChatEngine,
    private val chatRepository: ChatRepository,
    private val modelRepository: IModelRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

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

    fun checkAndInitModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ChatUiState.ModelLoading
            
            // ========== Android 11+兼容: 仅使用应用专属目录 ==========
            // 原因: Android 11+分区存储限制,Native C++无法读取/sdcard下的文件
            // 即使有READ_EXTERNAL_STORAGE权限也不行!
            // 唯一可行路径: /sdcard/Android/data/{package}/files/
            
            val appSpecificPath = context.getExternalFilesDir(null)?.let { 
                File(it, "MiniCPM4-0.5B-MNN")
            }
            
            if (appSpecificPath != null && appSpecificPath.exists() && appSpecificPath.isDirectory) {
                // 模型目录存在，直接初始化
                Log.d(TAG, "Found model at: ${appSpecificPath.absolutePath}")
                initializeModel(appSpecificPath.absolutePath)
            } else {
                val targetPath = appSpecificPath?.absolutePath ?: "unknown"
                _uiState.value = ChatUiState.ModelError(
                    "模型文件未找到！\n\n" +
                    "目标路径: $targetPath\n\n" +
                    "请使用以下命令推送模型:\n" +
                    "1. adb push MiniCPM4-0.5B-MNN /sdcard/\n" +
                    "2. adb shell mkdir -p /sdcard/Android/data/com.example.aiautominitest/files/\n" +
                    "3. adb shell cp -r /sdcard/MiniCPM4-0.5B-MNN /sdcard/Android/data/com.example.aiautominitest/files/\n\n" +
                    "或运行: .\\push_model.ps1"
                )
            }
            
            /* ========== 原Asset复制逻辑(已注释) ==========
            val assetCopier = AssetModelCopier(context)
            
            // Check if model files are already copied to external storage
            if (assetCopier.isModelCopied()) {
                // Model already exists, initialize directly
                val modelPath = assetCopier.getTargetDirectory().absolutePath
                initializeModel(modelPath)
            } else {
                // Need to copy from assets first
                _uiState.value = ChatUiState.ModelCopying(
                    currentFile = "准备复制...",
                    currentIndex = 0,
                    totalFiles = 0
                )
                
                val result = assetCopier.copyModelFiles { fileName, currentIndex, totalFiles ->
                    _uiState.value = ChatUiState.ModelCopying(
                        currentFile = fileName,
                        currentIndex = currentIndex,
                        totalFiles = totalFiles
                    )
                }
                
                result.fold(
                    onSuccess = { modelPath ->
                        // Copy succeeded, now initialize model
                        _uiState.value = ChatUiState.ModelLoading
                        initializeModel(modelPath)
                    },
                    onFailure = { error ->
                        _uiState.value = ChatUiState.ModelError(
                            "模型文件复制失败: ${error.message}"
                        )
                    }
                )
            }
            ========== 原Asset复制逻辑结束 ========== */
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
