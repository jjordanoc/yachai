package com.jjordanoc.yachai.llm

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.jjordanoc.yachai.data.ModelConfig
import com.jjordanoc.yachai.ui.screens.ResultListener

private const val LLM_TAG = "YachAIMediaPipeDataSource"


class MediaPipeLlmDataSource(
    private val context: Context,
    private val modelConfig: ModelConfig
) : LlmDataSource {

    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null

    override suspend fun initialize() {
        Log.d(LLM_TAG, "Initializing MediaPipe model...")
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelConfig.modelPath)
                .setMaxTokens(modelConfig.maxTokens)
                .setPreferredBackend(
                    LlmInference.Backend.CPU
                )
                .setMaxNumImages(if (modelConfig.supportImage) 1 else 0)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            resetSession()
            Log.d(LLM_TAG, "MediaPipe model initialization successful.")
        } catch (e: Exception) {
            Log.e(LLM_TAG, "Model initialization failed: ${e.message}", e)
            throw e // Re-throw the exception to be caught by the ViewModel
        }
    }

    private fun resetSession() {
        llmInference?.let { engine ->
            try {
                session?.close()
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(modelConfig.topK)
                    .setTopP(modelConfig.topP)
                    .setTemperature(modelConfig.temperature)
                    .setGraphOptions(
                        GraphOptions.builder()
                            .setEnableVisionModality(modelConfig.supportImage)
                            .build()
                    )
                    .build()
                session = LlmInferenceSession.createFromOptions(engine, sessionOptions)
                Log.d(LLM_TAG, "Session reset successful")
            } catch (e: Exception) {
                Log.e(LLM_TAG, "Failed to reset session", e)
            }
        }
    }

    override fun runInference(input: String, images: List<Bitmap>, resultListener: ResultListener) {
        val currentSession = session
        if (currentSession == null) {
            resultListener("Error: Model is not yet initialized or failed to load. Please wait or try again.", true)
            return
        }

        try {
            if (input.trim().isNotEmpty()) {
                currentSession.addQueryChunk(input)
            }

            for (image in images) {
                currentSession.addImage(BitmapImageBuilder(image).build())
            }

            currentSession.generateResponseAsync(resultListener)

        } catch (e: Exception) {
            Log.e(LLM_TAG, "Inference failed: ${e.message}", e)
            resultListener("Error: ${e.message ?: "Inference failed"}", true)
        }
    }

    override fun sizeInTokens(text: String): Int {
        return llmInference?.sizeInTokens(text) ?: -1
    }

    override fun cleanUp() {
        session?.close()
        llmInference?.close()
        Log.d(LLM_TAG, "MediaPipe cleanup completed.")
    }
}