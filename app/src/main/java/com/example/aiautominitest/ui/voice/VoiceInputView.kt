package com.example.aiautominitest.ui.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.aiautominitest.databinding.ViewVoiceButtonBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 语音输入视图组件
 * 支持长按录音、实时识别显示
 */
class VoiceInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewVoiceButtonBinding
    private var listener: VoiceInputListener? = null
    private var isRecording = false
    private var recordingStartTime = 0L

    init {
        binding = ViewVoiceButtonBinding.inflate(LayoutInflater.from(context), this, true)
        setupViews()
    }

    private fun setupViews() {
        // 设置长按录音逻辑
        binding.btnVoiceInput.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkMicrophonePermission()) {
                        startRecording()
                    } else {
                        requestMicrophonePermission()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) {
                        stopRecording()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 开始录音
     */
    private fun startRecording() {
        isRecording = true
        recordingStartTime = System.currentTimeMillis()

        // 更新UI
        binding.tvVoiceStatus.text = "正在识别..."
        binding.btnVoiceInput.isPressed = true
        binding.tvRecordingTime.visibility = View.VISIBLE
        binding.tvRecognizedText.visibility = View.VISIBLE
        binding.tvRecognizedText.text = ""

        // 开始更新录音时长
        updateRecordingTime()

        // 通知监听器
        listener?.onRecordingStart()
    }

    /**
     * 停止录音
     */
    private fun stopRecording() {
        isRecording = false

        // 更新UI
        binding.tvVoiceStatus.text = "按住说话"
        binding.btnVoiceInput.isPressed = false
        binding.tvRecordingTime.visibility = View.GONE

        // 通知监听器
        listener?.onRecordingStop()
    }

    /**
     * 更新识别文本
     */
    fun updateRecognizedText(text: String) {
        binding.tvRecognizedText.text = text
    }

    /**
     * 显示错误信息
     */
    fun showError(error: String) {
        binding.tvVoiceStatus.text = error
        binding.tvVoiceStatus.setTextColor(
            ContextCompat.getColor(context, android.R.color.holo_red_dark)
        )

        // 3秒后恢复
        postDelayed({
            if (!isRecording) {
                binding.tvVoiceStatus.text = "按住说话"
                binding.tvVoiceStatus.setTextColor(
                    ContextCompat.getColor(context, android.R.color.secondary_text_dark)
                )
            }
        }, 3000)
    }

    /**
     * 更新录音时长显示
     */
    private fun updateRecordingTime() {
        if (!isRecording) return

        val duration = (System.currentTimeMillis() - recordingStartTime) / 1000
        val minutes = duration / 60
        val seconds = duration % 60
        binding.tvRecordingTime.text = String.format("%02d:%02d", minutes, seconds)

        // 每秒更新一次
        postDelayed({ updateRecordingTime() }, 1000)
    }

    /**
     * 检查麦克风权限
     */
    private fun checkMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求麦克风权限
     */
    private fun requestMicrophonePermission() {
        MaterialAlertDialogBuilder(context)
            .setTitle("需要麦克风权限")
            .setMessage("语音识别功能需要使用麦克风权限，请在设置中授予。")
            .setPositiveButton("去设置") { _, _ ->
                listener?.onPermissionRequired()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 设置监听器
     */
    fun setVoiceInputListener(listener: VoiceInputListener) {
        this.listener = listener
    }

    /**
     * 重置状态
     */
    fun reset() {
        isRecording = false
        binding.tvVoiceStatus.text = "按住说话"
        binding.tvRecognizedText.text = ""
        binding.tvRecordingTime.visibility = View.GONE
        binding.tvRecognizedText.visibility = View.GONE
        binding.btnVoiceInput.isPressed = false
    }

    /**
     * 语音输入监听器
     */
    interface VoiceInputListener {
        /**
         * 开始录音
         */
        fun onRecordingStart()

        /**
         * 停止录音
         */
        fun onRecordingStop()

        /**
         * 需要麦克风权限
         */
        fun onPermissionRequired()
    }
}
