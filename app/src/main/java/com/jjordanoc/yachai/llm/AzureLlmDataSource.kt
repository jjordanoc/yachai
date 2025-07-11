package com.jjordanoc.yachai.llm

import android.graphics.Bitmap
import com.jjordanoc.yachai.ui.screens.ResultListener
import com.jjordanoc.yachai.utils.api.AzureOpenAIClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AzureLlmDataSource : LlmDataSource {
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun initialize() {
        // No-op for Azure client as it's a singleton and always "ready"
    }

    override fun runInference(input: String, images: List<Bitmap>, resultListener: ResultListener) {
        scope.launch {
            try {
                // AzureOpenAIClient only supports one image.
                val image = images.firstOrNull()
                val response = AzureOpenAIClient.callAzureOpenAI(prompt = input, bitmap = image)
                resultListener(response, true)
            } catch (e: Exception) {
                resultListener("Error: ${e.message}", true)
            }
        }
    }

    override fun sizeInTokens(text: String): Int {
        // Not implemented for Azure, return a placeholder
        return -1
    }

    override fun cleanUp() {
        // No-op for Azure client as it's a singleton object
    }
} 