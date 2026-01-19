package com.example.aiautominitest.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aiautominitest.App
import com.example.aiautominitest.asr.AsrResult
import com.example.aiautominitest.asr.SherpaOnnxAsrEngine
import com.example.aiautominitest.databinding.ActivityChatBinding

import com.example.aiautominitest.ui.voice.VoiceInputView
import android.util.Log
import com.example.aiautominitest.utils.AssetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 带语音功能的聊天Activity
 * 集成了Sherpa-ONNX离线语音识别
 */
class ChatActivityWithVoice : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter
    private val asrEngine = SherpaOnnxAsrEngine()


    private var isVoiceMode = false
    private var recognizedText = ""
    
    // 标记语音引擎是否准备就绪
    private var isAsrReady = false

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 100
        private const val REQUEST_AUDIO_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        val container = (application as App).container
        viewModel = ChatViewModel(
            container.chatEngine,
            container.chatRepository,
            container.modelRepository
        )

        setupRecyclerView()
        setupInputArea()
        setupVoiceInput()
        observeUiState()
        checkPermissions()
        prepareModels()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivityWithVoice).apply {
                stackFromEnd = true
            }
            adapter = this@ChatActivityWithVoice.adapter
        }
    }

    private fun setupInputArea() {
        // Send button
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // IME action
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // Clear history
        binding.fabClearHistory.setOnClickListener {
            viewModel.clearChatHistory()
            Toast.makeText(this, "聊天记录已清空", Toast.LENGTH_SHORT).show()
        }

        // Input mode toggle
        binding.btnInputModeToggle.setOnClickListener {
            toggleInputMode()
        }
    }

    private fun setupVoiceInput() {
        binding.voiceInputView.setVoiceInputListener(object : VoiceInputView.VoiceInputListener {
            override fun onRecordingStart() {
                startVoiceRecognition()
            }

            override fun onRecordingStop() {
                stopVoiceRecognition()
            }

            override fun onPermissionRequired() {
                requestAudioPermission()
            }
        })
    }

    /**
     * 切换输入模式（文字/语音）
     */
    private fun toggleInputMode() {
        isVoiceMode = !isVoiceMode

        if (isVoiceMode) {
            // 切换到语音模式
            if (!checkAudioPermission()) {
                requestAudioPermission()
                isVoiceMode = false
                return
            }

            binding.textInputLayout.visibility = View.GONE
            binding.btnSend.visibility = View.GONE
            binding.voiceInputView.visibility = View.VISIBLE
            binding.btnInputModeToggle.setIconResource(android.R.drawable.ic_menu_edit)
            Toast.makeText(this, "语音输入模式", Toast.LENGTH_SHORT).show()
        } else {
            // 切换到文字模式
            binding.textInputLayout.visibility = View.VISIBLE
            binding.btnSend.visibility = View.VISIBLE
            binding.voiceInputView.visibility = View.GONE
            binding.btnInputModeToggle.setIconResource(android.R.drawable.ic_btn_speak_now)
        }
    }

    /**
     * 准备模型文件
     */
    private fun prepareModels() {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@ChatActivityWithVoice, "正在准备语音模型...", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 列出 assets 根目录内容，帮助调试
                val assets = assets.list("") ?: emptyArray()
                Log.i("ChatActivity", "Assets root: ${assets.joinToString(", ")}")

                // 尝试从 assets 拷贝模型
                // 优先尝试 sherpa-onnx-models，如果失败尝试 sherpa-model
                // 尝试从 assets 拷贝模型
                // 优先尝试 sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20-mobile
                var assetPath = "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20-mobile"
                var destPath = AssetUtils.copyAssetFolder(this@ChatActivityWithVoice, assetPath)

                if (destPath == null) {
                    Log.w("ChatActivity", "New model not found, trying sherpa-onnx-models")
                    assetPath = "sherpa-onnx-models"
                    destPath = AssetUtils.copyAssetFolder(this@ChatActivityWithVoice, assetPath)
                }
                
                withContext(Dispatchers.Main) {
                    if (destPath != null) {
                        Log.i("ChatActivity", "Models copied to $destPath")
                        // 模型拷贝成功，尝试智能探测模型路径
                        val asrPath = findAsrModelPath(destPath)
                        
                        // 初始化 ASR
                        if (asrPath != null) {
                            initializeAsrEngine(asrPath)
                        } else {
                            // Fallback to strict structure assuming /zh
                            Log.w("ChatActivity", "ASR model auto-detection failed, trying default structure")
                            initializeAsrEngine("$destPath/zh")
                        }


                    } else {
                        // 拷贝失败
                        Log.e("ChatActivity", "Failed to copy assets. Available assets: ${assets.joinToString(", ")}")
                        Toast.makeText(
                            this@ChatActivityWithVoice,
                            "模型文件拷贝失败。请确认 assets 目录下存在 sherpa-onnx-models 或 sherpa-model",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error preparing models", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivityWithVoice, "模型准备出错: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun findAsrModelPath(basePath: String): String? {
        val dir = java.io.File(basePath)
        // 递归查找含有 tokens.txt 的目录
        val found = dir.walkTopDown().find { file ->
            file.isDirectory && java.io.File(file, "tokens.txt").exists() && 
            (java.io.File(file, "encoder-epoch-99-avg-1.onnx").exists() || 
             java.io.File(file, "encoder-epoch-99-avg-1.int8.onnx").exists() ||
             java.io.File(file, "encoder.onnx").exists())
        }
        return found?.absolutePath
    }



    /**
     * 初始化ASR引擎
     */
    private fun initializeAsrEngine(modelPath: String) {
        lifecycleScope.launch {
            try {
                Log.i("ChatActivity", "Initializing ASR with path: $modelPath")
                val success = asrEngine.init(modelPath)
                if (success) {
                    isAsrReady = true
                    Toast.makeText(this@ChatActivityWithVoice, "语音识别就绪", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("ChatActivity", "ASR init failed for path: $modelPath")
                    Toast.makeText(
                        this@ChatActivityWithVoice,
                        "语音识别引擎初始化失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "ASR init exception", e)
            }
        }
    }



    /**
     * 开始语音识别
     */
    private fun startVoiceRecognition() {
        if (!isAsrReady) {
            Toast.makeText(this, "语音识别未初始化", Toast.LENGTH_SHORT).show()
            return
        }
        
        recognizedText = ""

        lifecycleScope.launch {
            asrEngine.startRecognition()
                .collect { result ->
                    when (result) {
                        is AsrResult.Partial -> {
                            recognizedText = result.text
                            binding.voiceInputView.updateRecognizedText(recognizedText)

                            // 检测到端点，自动发送
                            if (result.isEndpoint && recognizedText.isNotEmpty()) {
                                stopVoiceRecognition()
                                sendMessage(recognizedText)
                                binding.voiceInputView.reset()
                                recognizedText = ""
                            }
                        }
                        is AsrResult.Final -> {
                            recognizedText = result.text
                            binding.voiceInputView.updateRecognizedText(recognizedText)
                        }
                        is AsrResult.Error -> {
                            binding.voiceInputView.showError(result.error)
                        }
                    }
                }
        }
    }

    /**
     * 停止语音识别
     */
    private fun stopVoiceRecognition() {
        asrEngine.stopRecognition()

        // 如果有识别结果，发送消息
        if (recognizedText.isNotEmpty()) {
            sendMessage(recognizedText)
            binding.voiceInputView.reset()
            recognizedText = ""
        }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            sendMessage(message)
            binding.etMessage.text?.clear()
        }
    }

    private fun sendMessage(text: String) {
        if (text.isNotEmpty()) {
            viewModel.sendMessage(text)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: ChatUiState) {
        when (state) {
            is ChatUiState.Idle -> {
                binding.tvModelStatus.text = "模型状态: 空闲"
                binding.progressModelLoading.visibility = View.GONE
            }
            is ChatUiState.ModelLoading -> {
                binding.tvModelStatus.text = "模型状态: 加载中..."
                binding.progressModelLoading.visibility = View.VISIBLE
                binding.btnSend.isEnabled = false
            }
            is ChatUiState.ModelReady -> {
                binding.tvModelStatus.text = "模型状态: 就绪 ✓"
                binding.progressModelLoading.visibility = View.GONE
                binding.btnSend.isEnabled = true
            }
            is ChatUiState.ModelError -> {
                binding.tvModelStatus.text = "模型错误: ${state.error}"
                binding.progressModelLoading.visibility = View.GONE
                binding.btnSend.isEnabled = false
                Toast.makeText(this, state.error, Toast.LENGTH_LONG).show()
            }
            is ChatUiState.Chatting -> {
                binding.tvModelStatus.text = if (state.isGenerating) {
                    "模型状态: 生成中..."
                } else {
                    "模型状态: 就绪 ✓"
                }
                binding.btnSend.isEnabled = !state.isGenerating

                // Update messages list
                val displayMessages = state.messages.toMutableList()

                // Add temporary generating message
                if (state.isGenerating && state.currentResponse.isNotEmpty()) {
                    val tempMessage = com.example.aiautominitest.data.model.ChatMessage(
                        id = -1,
                        role = com.example.aiautominitest.data.model.ChatMessage.ROLE_ASSISTANT,
                        content = state.currentResponse,
                        status = 0
                    )
                    displayMessages.add(tempMessage)
                }

                adapter.submitList(displayMessages) {
                    // Auto-scroll to bottom
                    if (displayMessages.isNotEmpty()) {
                        binding.recyclerViewMessages.smoothScrollToPosition(displayMessages.size - 1)
                    }
                }


            }
        }
    }

    /**
     * 检查麦克风权限
     */
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求麦克风权限
     */
    private fun requestAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            // 显示解释
            Toast.makeText(
                this,
                "语音识别需要麦克风权限",
                Toast.LENGTH_LONG
            ).show()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_AUDIO_PERMISSION
        )
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()

            // Storage permissions
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions.toTypedArray(),
                    REQUEST_STORAGE_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要存储权限以访问模型文件", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "麦克风权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要麦克风权限进行语音识别", Toast.LENGTH_LONG).show()
                    // 提示用户去设置页面
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        )
                    ) {
                        // 用户选择了"不再询问"
                        openAppSettings()
                    }
                }
            }
        }
    }

    /**
     * 打开应用设置页面
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        asrEngine.release()

    }
}
