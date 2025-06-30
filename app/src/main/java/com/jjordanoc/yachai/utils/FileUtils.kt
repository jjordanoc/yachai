package com.jjordanoc.yachai.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val MODEL_NAME = "gemma-3n-E2B-it-litert-preview.task"

    fun getModelFile(context: Context): File {
        return File(context.cacheDir, MODEL_NAME)
    }

    fun isModelDownloaded(context: Context): Boolean {
        return getModelFile(context).exists()
    }

    fun getModelPath(context: Context): String {
        val modelFile = getModelFile(context)
        if (modelFile.exists()) {
            return modelFile.absolutePath
        }

        // Fallback to bundled asset if not downloaded
        val assetModelFile = File(context.cacheDir, "gemma-3n.task")
        if (!assetModelFile.exists()) {
            val inputStream = context.assets.open("gemma-3n.task")
            val outputStream = FileOutputStream(assetModelFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        }
        return assetModelFile.absolutePath
    }
} 