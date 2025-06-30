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
import com.jjordanoc.yachai.utils.FileUtils
import com.jjordanoc.yachai.utils.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ResultUiState {
    object Loading : ResultUiState
    data class Success(val explanation: String) : ResultUiState
    data class Error(val message: String) : ResultUiState
}

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun runInference(imageUri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = ResultUiState.Loading
            try {
                Log.d(TAG, "Starting inference for image: $imageUri")
                val context = getApplication<Application>().applicationContext
                val modelPath = FileUtils.getModelPath(context)
                Log.d(TAG, "Using model at path: $modelPath")

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxNumImages(1)
                    .build()

                val sessionOptions =
                    LlmInferenceSession.LlmInferenceSessionOptions.builder()
                        .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                        .build()

                LlmInference.createFromOptions(context, options).use { llmInference ->
                    Log.d(TAG, "LlmInference created. Creating session.")
                    LlmInferenceSession.createFromOptions(llmInference, sessionOptions).use { session ->
                        Log.d(TAG, "Session created. Adding query chunk and image.")
                        val prompt = "Categoría: [Álgebra, Geometría, Aritmética]\n\nExplica cómo resolver este problema de matemáticas paso a paso. Sé claro y didáctico."
                        val bitmap = uriToBitmap(context, imageUri)
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        
                        session.addQueryChunk(prompt)
                        session.addImage(mpImage)
                        
                        Log.d(TAG, "Generating response from session.")
                        val result = session.generateResponse()
                        Log.d(TAG, "Inference successful.")

                        val category = result.lines().firstOrNull()?.substringAfter("Categoría: ")?.trim()
                        if (category != null) {
                            Log.d(TAG, "Found category: $category. Saving topic.")
                            saveTopic(category)
                        }

                        _uiState.value = ResultUiState.Success(result)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed: ${e.stackTraceToString()}")
                _uiState.value = ResultUiState.Error(e.localizedMessage ?: "Unknown error during inference")
            }
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

    private fun saveTopic(topic: String) {
        val context = getApplication<Application>().applicationContext
        val sharedPref = context.getSharedPreferences("YachAITopics", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            val topics = sharedPref.getStringSet("topics", mutableSetOf()) ?: mutableSetOf()
            topics.add(topic)
            putStringSet("topics", topics)
            apply()
        }
    }
} 