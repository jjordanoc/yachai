package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.jjordanoc.yachai.llm.data.ModelDownloadStatus
import com.jjordanoc.yachai.llm.data.Models
import com.jjordanoc.yachai.llm.data.getLocalPath
import com.jjordanoc.yachai.utils.TAG
import com.jjordanoc.yachai.utils.SettingsManager
import com.jjordanoc.yachai.utils.api.AzureOpenAIClient
import com.jjordanoc.yachai.utils.ocr.MathOCRManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Job


sealed interface ChatMessage {
    data class UserMessage(val text: String = "", val image: Bitmap? = null) : ChatMessage
    data class ModelMessage(val text: String) : ChatMessage
    data class ErrorMessage(val text: String) : ChatMessage
}

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Loading : ChatUiState
    data class Success(val messages: List<ChatMessage>) : ChatUiState
    data class Error(val message: String) : ChatUiState
}


class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Success(listOf()))
    val uiState = _uiState.asStateFlow()

    private val ocrManager = MathOCRManager(application)
    private var llmInference: LlmInference? = null
    private val model = Models.GEMMA_3N_E2B_VISION
    private var initializationJob: Job? = null

    init {
        initializeModel()
    }

    private fun addMessage(message: ChatMessage) {
        val currentMessages = (_uiState.value as? ChatUiState.Success)?.messages ?: emptyList()
        _uiState.value = ChatUiState.Success(currentMessages + message)
    }

    private fun initializeModel() {
        initializationJob = viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val settingsManager = SettingsManager(context)
            val modelPath = model.getLocalPath(context)
            val modelFile = java.io.File(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file does not exist at: $modelPath")
                _uiState.value = ChatUiState.Error("Model is not downloaded. Please go back and download it first.")
                return@launch
            }

            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .setMaxNumImages(1)
                    .setPreferredBackend(
                        if (settingsManager.isGpuEnabled()) LlmInference.Backend.GPU
                        else LlmInference.Backend.CPU
                    )
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to initialize model: ${e.localizedMessage}")
            }
        }
    }

    fun processImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            initializationJob?.join() // Wait for the model to be initialized

            _uiState.value = ChatUiState.Loading
            val context = getApplication<Application>().applicationContext
            try {
                val bitmap = uriToBitmap(context, uri)
                addMessage(ChatMessage.UserMessage(image = bitmap))

                // Run OCR and display result
                val ocrResult = ocrManager.extractMathFromImage(bitmap).await()
                val extractedText = ocrResult.text
                if (extractedText.isNotBlank()) {
                    addMessage(ChatMessage.ModelMessage("OCR Result: $extractedText"))
                } else {
                    addMessage(ChatMessage.ModelMessage("OCR couldn't find any text."))
                }
                
                // Run Azure inference and display result
                // runAzureInference(bitmap, extractedText)
                runInferenceWithImage(bitmap)

            } catch (e: Exception) {
                Log.e(TAG, "OCR/Inference failed: ${e.stackTraceToString()}")
                addMessage(ChatMessage.ErrorMessage("An error occurred: ${e.localizedMessage}"))
            } finally {
                // Ensure loading state is cleared
                 val currentMessages = (_uiState.value as? ChatUiState.Success)?.messages ?: emptyList()
                _uiState.value = ChatUiState.Success(currentMessages)
            }
        }
    }

    /*
    private fun runAzureInference(bitmap: Bitmap, ocrText: String) {
        try {
            val prompt = ""
                You are a friendly and encouraging math tutor. The user has provided an image.".
                Based on the image, how can I solve this problem?
                Provide a clear, step-by-step explanation.
                "" .trimIndent()
            
            val result = AzureOpenAIClient.callAzureOpenAI(prompt = prompt, bitmap = bitmap)
            addMessage(ChatMessage.ModelMessage("LLM Result:\n$result"))
        } catch (e: Exception) {
             Log.e(TAG, "Azure Inference failed: ${e.stackTraceToString()}")
            addMessage(ChatMessage.ErrorMessage("Azure Inference failed: ${e.localizedMessage}"))
        }
    }
    */
    
    private suspend fun runInferenceWithImage(bitmap: Bitmap) {
        if (llmInference == null) {
            addMessage(ChatMessage.ErrorMessage("Model is not initialized."))
            return
        }

        try {
            val prompt_animation_geometry = ""
            val prompt = prompt_animation_geometry
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setGraphOptions(
                        GraphOptions.builder()
                            .setEnableVisionModality(true)
                            .build()
                    )
                    .build()

            LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions).use { session ->
                session.addQueryChunk(prompt)
                session.addImage(mpImage)
                val result = session.generateResponse()
                addMessage(ChatMessage.ModelMessage("LLM Result:\n$result"))
            }

        } catch (e: Exception) {
             Log.e(TAG, "Inference failed: ${e.stackTraceToString()}")
            addMessage(ChatMessage.ErrorMessage("LLM Inference failed: ${e.localizedMessage}"))
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    override fun onCleared() {
        super.onCleared()
        llmInference?.close()
    }
} 