package com.jjordanoc.yachai.llm

import android.content.Context
import android.graphics.Bitmap
import com.jjordanoc.yachai.data.ModelConfig

object LlmHelper {
    private var activeDataSource: LlmDataSource? = null

    enum class DataSourceType {
        MEDIAPIPE,
        AZURE
    }

    suspend fun switchDataSource(type: DataSourceType, context: Context, modelConfig: ModelConfig? = null) {
        activeDataSource?.cleanUp()
        val newDataSource = when (type) {
            DataSourceType.MEDIAPIPE -> {
                if (modelConfig == null) {
                    throw IllegalArgumentException("ModelConfig is required for MediaPipe")
                }
                MediaPipeLlmDataSource(context, modelConfig)
            }
            DataSourceType.AZURE -> AzureLlmDataSource()
        }
        newDataSource.initialize()
        activeDataSource = newDataSource
    }

    fun runInference(input: String, images: List<Bitmap> = emptyList(), resultListener: ResultListener) {
        activeDataSource?.runInference(input, images, resultListener)
            ?: resultListener("Error: No data source selected", true)
    }

    fun sizeInTokens(text: String): Int {
        return activeDataSource?.sizeInTokens(text) ?: -1
    }

    fun cleanUp() {
        activeDataSource?.cleanUp()
        activeDataSource = null
    }
} 