package com.jjordanoc.yachai.ui.screens

import android.content.Context
import android.graphics.Bitmap
import com.jjordanoc.yachai.data.ModelConfig

object LlmHelper {
    private var activeDataSource: LlmDataSource? = null

    enum class DataSourceType {
        MEDIAPIPE,
        AZURE
    }

    fun switchDataSource(type: DataSourceType, context: Context, modelConfig: ModelConfig? = null) {
        activeDataSource?.cleanUp()
        activeDataSource = when (type) {
            DataSourceType.MEDIAPIPE -> {
                if (modelConfig == null) {
                    throw IllegalArgumentException("ModelConfig is required for MediaPipe")
                }
                MediaPipeLlmDataSource(context, modelConfig)
            }
            DataSourceType.AZURE -> AzureLlmDataSource()
        }
    }

    fun runInference(input: String, images: List<Bitmap> = emptyList(), resultListener: ResultListener) {
        activeDataSource?.runInference(input, images, resultListener)
            ?: resultListener("Error: No data source selected", true)
    }

    fun cleanUp() {
        activeDataSource?.cleanUp()
        activeDataSource = null
    }
} 