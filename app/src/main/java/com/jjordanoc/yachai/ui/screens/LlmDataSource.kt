package com.jjordanoc.yachai.ui.screens

import android.graphics.Bitmap

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit

interface LlmDataSource {
    fun runInference(
        input: String,
        images: List<Bitmap> = emptyList(),
        resultListener: ResultListener
    )
    fun cleanUp()
} 