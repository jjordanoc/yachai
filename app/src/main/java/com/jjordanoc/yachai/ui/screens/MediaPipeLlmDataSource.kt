package com.jjordanoc.yachai.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.jjordanoc.yachai.data.ModelConfig
import com.jjordanoc.yachai.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val LLM_TAG = "YachAIMediaPipeDataSource"

class MediaPipeLlmDataSource(
    private val context: Context,
    private val modelConfig: ModelConfig
) : LlmDataSource {

    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null

    init {
        initialize()
    }

    private fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(LLM_TAG, "Initializing MediaPipe model...")
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelConfig.modelPath)
                    .setMaxTokens(modelConfig.maxTokens)
                    .setPreferredBackend(
                        if (modelConfig.useGpu) LlmInference.Backend.GPU
                        else LlmInference.Backend.CPU
                    )
                    .setMaxNumImages(if (modelConfig.supportImage) 1 else 0)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                resetSession()
                Log.d(LLM_TAG, "MediaPipe model initialization successful.")
            } catch (e: Exception) {
                Log.e(LLM_TAG, "Model initialization failed: ${e.message}", e)
                // We can't easily bubble this up to the UI from a fire-and-forget coroutine.
                // The session will remain null, and runInference will report the error.
            }
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
            resultListener("Error: ${cleanUpErrorMessage(e.message ?: "Inference failed")}", true)
        }
    }

    override fun cleanUp() {
        session?.close()
        llmInference?.close()
        Log.d(LLM_TAG, "MediaPipe cleanup completed.")
    }

    private fun cleanUpErrorMessage(message: String): String {
        return when {
            message.contains("Out of memory") -> "Device does not have enough memory to run this model. Try using a smaller model or restart the app."
            message.contains("OpenCL") -> "GPU acceleration not available. The model will use CPU processing."
            message.contains("libvndksupport.so") -> "GPU drivers not available. Using CPU processing instead."
            message.contains("Failed to initialize") -> "Model failed to load. Please check if the model file is valid."
            else -> message
        }
    }
} 