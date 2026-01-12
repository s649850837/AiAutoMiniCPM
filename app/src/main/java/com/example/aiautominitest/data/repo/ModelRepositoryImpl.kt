package com.example.aiautominitest.data.repo

import android.content.Context
import com.example.aiautominitest.data.model.ModelConfig

import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class ModelRepositoryImpl(private val context: Context) : IModelRepository {

    private val MODEL_DIR = "models"
    private val MODEL_NAME = "minicpm.mnn"
    private val TOKENIZER_NAME = "tokenizer.txt"
    private val CONFIG_URL = "https://mock.server/config.json" // Mock

    private var currentConfig: ModelConfig? = null

    override suspend fun checkModelUpdate(): Boolean {
        // Mock fetch config
        // In real app, use Retrofit
        currentConfig = ModelConfig(
            modelUrl = "https://example.com/minicpm.mnn", // Replace with real URL later
            modelMd5 = "dummy_md5",
            modelSize = 1024 * 1024 * 1024, // 1GB
            tokenizerUrl = "https://example.com/tokenizer.txt",
            tokenizerMd5 = "dummy_md5_tokenizer",
            version = 1
        )
        return true
    }

    override suspend fun downloadModel(): Flow<DownloadState> = callbackFlow {
        if (currentConfig == null) {
            checkModelUpdate()
        }
        val config = currentConfig!!
        
        val modelPath = getModelFile().absolutePath
        val tokenizerPath = getTokenizerFile().absolutePath

        // Simple sequential download for demo
        // In stable version, we should manage queue
        
        val listener = object : FileDownloadListener() {
            override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {}
            
            override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                if (totalBytes > 0) {
                    val percent = (soFarBytes.toFloat() / totalBytes * 100).toInt()
                     trySend(DownloadState.Downloading(percent, task?.speed?.toLong() ?: 0))
                }
            }

            override fun completed(task: BaseDownloadTask?) {
                 // Check if both finished? 
                 // For simplicity, assumed just one huge task or sending Ready when all done
                 // Here we just send Ready for this task
                 trySend(DownloadState.Ready)
            }

            override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {}
            override fun error(task: BaseDownloadTask?, e: Throwable?) {
                trySend(DownloadState.Error(e?.message ?: "Unknown error"))
            }
            override fun warn(task: BaseDownloadTask?) {}
        }

        FileDownloader.getImpl().create(config.modelUrl)
            .setPath(modelPath)
            .setListener(listener)
            .start()

        awaitClose { 
            // Cleanup or pause
        }
    }

    override fun getModelPath(): String? {
        val file = getModelFile()
        return if (file.exists()) file.absolutePath else null
    }

    override fun getTokenizerPath(): String? {
        val file = getTokenizerFile()
        return if (file.exists()) file.absolutePath else null
    }

    override fun isModelReady(): Boolean {
        return getModelFile().exists() && getTokenizerFile().exists()
        // Add MD5 check logic here
    }

    private fun getModelFile(): File {
        val dir = File(context.filesDir, MODEL_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, MODEL_NAME)
    }

    private fun getTokenizerFile(): File {
        val dir = File(context.filesDir, MODEL_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, TOKENIZER_NAME)
    }
}
