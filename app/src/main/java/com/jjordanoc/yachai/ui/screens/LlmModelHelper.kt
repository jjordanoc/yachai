package com.jjordanoc.yachai.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.jjordanoc.yachai.utils.TAG

private const val LLM_TAG = "YachAILlmModelHelper"

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit
typealias CleanUpListener = () -> Unit

data class LlmModelInstance(val engine: LlmInference, var session: LlmInferenceSession)

data class ModelConfig(
    val modelPath: String,
    val maxTokens: Int = 512,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val temperature: Float = 0.8f,
    val supportImage: Boolean = true,
    val preferredBackend: LlmInference.Backend = LlmInference.Backend.GPU
)

object LlmModelHelper {
    // Store model instances and cleanup listeners
    private val modelInstances: MutableMap<String, LlmModelInstance> = mutableMapOf()
    private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

    fun initialize(
        context: Context, 
        modelConfig: ModelConfig,
        modelKey: String = "default",
        onDone: (String) -> Unit
    ) {
        Log.d(LLM_TAG, "Initializing model with key: $modelKey")
        
        try {
            // Clean up any existing instance
            cleanUp(modelKey)
            
            // Create LLM Inference options
            val optionsBuilder = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelConfig.modelPath)
                .setMaxTokens(modelConfig.maxTokens)
                .setPreferredBackend(modelConfig.preferredBackend)
                .setMaxNumImages(if (modelConfig.supportImage) 1 else 0)
            
            val options = optionsBuilder.build()

            // Create LLM Inference engine
            val llmInference = LlmInference.createFromOptions(context, options)

            // Create session with vision support if needed
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

            val session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
            
            // Store the instance
            modelInstances[modelKey] = LlmModelInstance(engine = llmInference, session = session)
            
            Log.d(LLM_TAG, "Model initialization successful")
            onDone("")
            
        } catch (e: Exception) {
            Log.e(LLM_TAG, "Model initialization failed: ${e.message}", e)
            val cleanMessage = cleanUpErrorMessage(e.message ?: "Unknown error")
            onDone(cleanMessage)
        }
    }

    fun resetSession(modelKey: String = "default", modelConfig: ModelConfig) {
        try {
            Log.d(LLM_TAG, "Resetting session for model: $modelKey")
            
            val instance = modelInstances[modelKey] ?: return
            
            // Close old session
            instance.session.close()
            
            // Create new session
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

            val newSession = LlmInferenceSession.createFromOptions(instance.engine, sessionOptions)
            instance.session = newSession
            
            Log.d(LLM_TAG, "Session reset successful")
        } catch (e: Exception) {
            Log.e(LLM_TAG, "Failed to reset session", e)
        }
    }

    fun runInference(
        input: String,
        resultListener: ResultListener,
        cleanUpListener: CleanUpListener,
        modelKey: String = "default",
        images: List<Bitmap> = emptyList()
    ) {
        val instance = modelInstances[modelKey]
        if (instance == null) {
            resultListener("Error: Model not initialized", true)
            return
        }

        // Set cleanup listener
        cleanUpListeners[modelKey] = cleanUpListener

        try {
            val session = instance.session
            
            // Add text input if not empty
            if (input.trim().isNotEmpty()) {
                session.addQueryChunk(input)
            }
            
            // Add images if provided
            for (image in images) {
                session.addImage(BitmapImageBuilder(image).build())
            }
            
            // Start async inference
            session.generateResponseAsync(resultListener)
            
        } catch (e: Exception) {
            Log.e(LLM_TAG, "Inference failed: ${e.message}", e)
            resultListener("Error: ${cleanUpErrorMessage(e.message ?: "Inference failed")}", true)
        }
    }

    fun cleanUp(modelKey: String = "default") {
        val instance = modelInstances.remove(modelKey) ?: return
        
        try {
            instance.session.close()
        } catch (e: Exception) {
            Log.e(LLM_TAG, "Failed to close session: ${e.message}")
        }

        try {
            instance.engine.close()
        } catch (e: Exception) {
            Log.e(LLM_TAG, "Failed to close engine: ${e.message}")
        }

        // Execute cleanup listener
        val onCleanUp = cleanUpListeners.remove(modelKey)
        onCleanUp?.invoke()
        
        Log.d(LLM_TAG, "Cleanup completed for model: $modelKey")
    }

    fun cleanUpAll() {
        val keys = modelInstances.keys.toList()
        for (key in keys) {
            cleanUp(key)
        }
    }

    private fun cleanUpErrorMessage(message: String): String {
        // Clean up common MediaPipe error messages to be more user-friendly
        return when {
            message.contains("Out of memory") -> "Device does not have enough memory to run this model. Try using a smaller model or restart the app."
            message.contains("OpenCL") -> "GPU acceleration not available. The model will use CPU processing."
            message.contains("libvndksupport.so") -> "GPU drivers not available. Using CPU processing instead."
            message.contains("Failed to initialize") -> "Model failed to load. Please check if the model file is valid."
            else -> message
        }
    }
} 