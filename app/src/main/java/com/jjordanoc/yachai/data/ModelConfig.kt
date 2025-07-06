package com.jjordanoc.yachai.data

data class ModelConfig(
    val modelPath: String,
    val maxTokens: Int = 2048,
    val topK: Int = 10,
    val topP: Float = 0.9f,
    val temperature: Float = 0.8f,
    val supportImage: Boolean = true,
    val useGpu: Boolean = true
) 