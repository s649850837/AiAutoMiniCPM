package com.example.aiautominitest.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * 音频录制器
 * 负责从麦克风采集音频数据，并以Flow形式提供给语音识别引擎
 */
class AudioRecorder {
    companion object {
        private const val TAG = "AudioRecorder"

        // Sherpa-ONNX 推荐的音频参数
        const val SAMPLE_RATE = 16000  // 16kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // 缓冲区大小：0.1秒的音频数据
        private const val BUFFER_SIZE_IN_SAMPLES = SAMPLE_RATE / 10
        private const val BYTES_PER_SAMPLE = 2  // 16-bit = 2 bytes
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    /**
     * 开始录音并返回音频数据流
     * @return Flow<FloatArray> 音频样本流 (归一化到 -1.0 到 1.0)
     */
    fun startRecording(): Flow<FloatArray> = flow {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return@flow
        }

        val bufferSizeInBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ).coerceAtLeast(BUFFER_SIZE_IN_SAMPLES * BYTES_PER_SAMPLE)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSizeInBytes
            ).apply {
                if (state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioRecord initialization failed")
                }
            }

            audioRecord?.startRecording()
            isRecording = true
            Log.i(TAG, "Recording started: $SAMPLE_RATE Hz, buffer size: $bufferSizeInBytes bytes")

            val audioBuffer = ShortArray(BUFFER_SIZE_IN_SAMPLES)

            while (coroutineContext.isActive && isRecording) {
                val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1

                if (readCount > 0) {
                    // 转换 Short 到 Float (归一化到 -1.0 到 1.0)
                    val floatBuffer = FloatArray(readCount) { i ->
                        audioBuffer[i] / 32768.0f
                    }
                    emit(floatBuffer)
                } else if (readCount == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Error reading audio: ERROR_INVALID_OPERATION")
                    break
                } else if (readCount == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Error reading audio: ERROR_BAD_VALUE")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording", e)
            throw e
        } finally {
            stopRecording()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 停止录音
     */
    fun stopRecording() {
        if (!isRecording) return

        isRecording = false

        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            Log.i(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            audioRecord = null
        }
    }

    /**
     * 检查是否正在录音
     */
    fun isRecording(): Boolean = isRecording
}
