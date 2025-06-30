package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.jjordanoc.yachai.utils.FileUtils
import com.jjordanoc.yachai.utils.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PracticeUiState {
    object Loading : PracticeUiState
    data class Problem(val text: String) : PracticeUiState
    data class Feedback(val isCorrect: Boolean, val message: String) : PracticeUiState
    data class Error(val message: String) : PracticeUiState
}

class PracticeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private lateinit var llmInference: LlmInference
    private var currentProblem: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing PracticeViewModel and loading model.")
                val context = getApplication<Application>().applicationContext
                val modelPath = FileUtils.getModelPath(context)
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                Log.d(TAG, "Model loaded successfully for PracticeViewModel.")
                generatePracticeProblem()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model in PracticeViewModel: ${e.stackTraceToString()}")
                _uiState.value = PracticeUiState.Error(e.localizedMessage ?: "Failed to load model")
            }
        }
    }

    fun generatePracticeProblem() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = PracticeUiState.Loading
            try {
                val topic = getTopic()
                Log.d(TAG, "Generating practice problem for topic: $topic")
                val prompt = "Genera un problema de práctica sobre $topic. Incluye solo el problema, sin la solución."
                val result = llmInference.generateResponse(prompt)
                currentProblem = result
                _uiState.value = PracticeUiState.Problem(result)
                Log.d(TAG, "Successfully generated problem: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate practice problem: ${e.stackTraceToString()}")
                _uiState.value = PracticeUiState.Error(e.localizedMessage ?: "Failed to generate problem")
            }
        }
    }

    fun checkAnswer(answer: String) {
        val problem = currentProblem ?: return
        viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.d(TAG, "Checking answer for problem: '$problem'. User answer: '$answer'")
                val prompt = "El problema es: '$problem'. La respuesta del estudiante es: '$answer'. ¿Es correcta la respuesta? Responde solo con 'Sí' o 'No'."
                val result = llmInference.generateResponse(prompt)
                val isCorrect = result.trim().equals("Sí", ignoreCase = true)
                val feedbackMessage = if (isCorrect) "¡Correcto! ¡Muy bien!" else "No es correcto. ¡Sigue intentando!"
                _uiState.value = PracticeUiState.Feedback(isCorrect, feedbackMessage)
                Log.d(TAG, "Answer check result: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check answer: ${e.stackTraceToString()}")
                _uiState.value = PracticeUiState.Error(e.localizedMessage ?: "Failed to check answer")
            }
        }
    }

    private fun getTopic(): String {
        val context = getApplication<Application>().applicationContext
        val sharedPref = context.getSharedPreferences("YachAITopics", Context.MODE_PRIVATE)
        val topics = sharedPref.getStringSet("topics", null)
        return topics?.randomOrNull() ?: "Aritmética"
    }
} 