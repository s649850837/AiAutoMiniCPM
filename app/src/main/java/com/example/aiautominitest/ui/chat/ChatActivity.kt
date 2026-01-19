package com.example.aiautominitest.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.example.aiautominitest.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 100
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
        observeUiState()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = this@ChatActivity.adapter
        }
    }

    private fun setupInputArea() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.fabClearHistory.setOnClickListener {
            viewModel.clearChatHistory()
            Toast.makeText(this, "聊天记录已清空", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            viewModel.sendMessage(message)
            binding.etMessage.text?.clear()
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

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()
            
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
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要存储权限以访问模型文件", Toast.LENGTH_LONG).show()
            }
        }
    }
}
