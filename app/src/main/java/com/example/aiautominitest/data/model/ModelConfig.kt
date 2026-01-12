package com.example.aiautominitest.data.model

data class ModelConfig(
    val modelUrl: String,
    val modelMd5: String,
    val modelSize: Long,
    val tokenizerUrl: String,
    val tokenizerMd5: String,
    val version: Int
)
