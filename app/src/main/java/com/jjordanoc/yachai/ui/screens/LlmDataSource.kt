package com.jjordanoc.yachai.ui.screens

import android.graphics.Bitmap

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit

interface LlmDataSource {
    suspend fun initialize()
    fun runInference(
        input: String,
        images: List<Bitmap> = emptyList(),
        resultListener: ResultListener
    )
    fun sizeInTokens(text: String): Int
    fun cleanUp()
} 