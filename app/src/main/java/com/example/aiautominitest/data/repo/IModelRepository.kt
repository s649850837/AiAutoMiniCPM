package com.example.aiautominitest.data.repo

import kotlinx.coroutines.flow.Flow

interface IModelRepository {
    suspend fun checkModelUpdate(): Boolean
    suspend fun downloadModel(): Flow<DownloadState>
    fun getModelPath(): String?
    fun getTokenizerPath(): String?
    fun isModelReady(): Boolean
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val currentSpeed: Long) : DownloadState()
    object Merging : DownloadState()
    object Unzipping : DownloadState()
    object Ready : DownloadState()
    data class Error(val message: String) : DownloadState()
}
