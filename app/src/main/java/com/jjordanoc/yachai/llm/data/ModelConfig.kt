package com.jjordanoc.yachai.llm.data

data class ModelConfig(
    val modelPath: String,
    val maxTokens: Int = 8192,
    val topK: Int = 10,
    val topP: Float = 0.9f,
    val temperature: Float = 0.8f,
    val supportImage: Boolean = true,
    val useGpu: Boolean = true
) 