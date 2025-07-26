package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.jjordanoc.yachai.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.jjordanoc.yachai.llm.data.Models
import com.jjordanoc.yachai.llm.data.getLocalPath
import com.jjordanoc.yachai.utils.SettingsManager
import com.jjordanoc.yachai.llm.data.ModelConfig
import com.jjordanoc.yachai.llm.LlmHelper
import com.jjordanoc.yachai.ui.screens.whiteboard.model.InterpretResponse
import com.jjordanoc.yachai.ui.screens.whiteboard.model.LlmResponse
import com.jjordanoc.yachai.ui.screens.whiteboard.systemPromptInterpret
import com.jjordanoc.yachai.ui.screens.whiteboard.systemPromptSocratic
import androidx.lifecycle.ViewModelProvider

enum class TutorialFlowState {
    INITIAL,
    INTERPRETING,
    AWAITING_CONFIRMATION,
    CHATTING
}

data class TutorialState(
    val textInput: String = "",
    val selectedImageUri: Uri? = null,
    val tutorMessage: String? = null,
    val subject: String = "",
    val flowState: TutorialFlowState = TutorialFlowState.INITIAL,
    val isModelLoading: Boolean = true,
    val showConfirmationFailureMessage: Boolean = false,
    val initialProblemStatement: String = "",
    val isAlpacaSpeaking: Boolean = false
)

class TutorialViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TutorialState())
    val uiState: StateFlow<TutorialState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    init {
        Log.d(TAG, "TutorialViewModel initialized.")
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val settingsManager = SettingsManager(context)
            val model = Models.GEMMA_3N_E2B_VISION

            val modelConfig = ModelConfig(
                modelPath = model.getLocalPath(context),
                useGpu = settingsManager.isGpuEnabled()
            )

            try {
                LlmHelper.switchDataSource(LlmHelper.DataSourceType.AZURE, context, modelConfig)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize model: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isModelLoading = false,
                        tutorMessage = "Error: Failed to load the model. Please restart the app."
                    )
                }
            } finally {
                _uiState.update { it.copy(isModelLoading = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        LlmHelper.cleanUp()
        Log.d(TAG, "TutorialViewModel cleared and LlmHelper cleaned up.")
    }

    fun triggerAlpacaSpeaking() {
        _uiState.update { it.copy(isAlpacaSpeaking = true) }
        
        // Stop speaking after 3 seconds (fallback for manual triggers)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(isAlpacaSpeaking = false) }
        }
    }
    
    fun startAlpacaSpeaking() {
        _uiState.update { it.copy(isAlpacaSpeaking = true) }
    }
    
    fun stopAlpacaSpeaking() {
        _uiState.update { it.copy(isAlpacaSpeaking = false) }
    }

    fun processLlmResponse(jsonString: String) {
        Log.d(TAG, "Processing LLM response: $jsonString")

        if (jsonString.startsWith("Error:")) {
            Log.e(TAG, "Received an error from LlmDataSource: $jsonString")
            _uiState.update { 
                it.copy(
                    tutorMessage = jsonString,
                    isAlpacaSpeaking = true
                )
            }
            return
        }

        try {
            val startIndex = jsonString.indexOf('{')
            val endIndex = jsonString.lastIndexOf('}')
            val cleanJsonString = if (startIndex != -1 && endIndex > startIndex) {
                jsonString.substring(startIndex, endIndex + 1)
            } else {
                Log.w(TAG, "Could not find a valid JSON object in the response. Trying to parse as is.")
                jsonString
            }

            val currentState = _uiState.value

            // Adapt parsing based flow state
            val response: LlmResponse = if (currentState.flowState == TutorialFlowState.INTERPRETING) {
                val interpretResponse = json.decodeFromString<InterpretResponse>(cleanJsonString)
                LlmResponse(
                    tutorMessage = interpretResponse.tutorMessage,
                    hint = null,
                    animation = emptyList() // No animations in tutorial screen
                )
            } else {
                json.decodeFromString<LlmResponse>(cleanJsonString)
            }

            Log.d(TAG, "LLM response parsed successfully: $response")

            val newFlowState = if (currentState.flowState == TutorialFlowState.INTERPRETING) {
                TutorialFlowState.AWAITING_CONFIRMATION
            } else {
                currentState.flowState
            }

            // Extract subject from interpretation response if this is the interpreting phase
            val extractedSubject = if (currentState.flowState == TutorialFlowState.INTERPRETING) {
                extractSubjectFromResponse(response.tutorMessage ?: "")
            } else {
                currentState.subject
            }

            _uiState.update { state ->
                state.copy(
                    tutorMessage = response.tutorMessage,
                    subject = extractedSubject,
                    flowState = newFlowState,
                    isAlpacaSpeaking = true
                ).also {
                    Log.d(TAG, "State updated with new tutor message, subject: '$extractedSubject', and alpaca speaking triggered.")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LLM response: $jsonString", e)
        }
    }

    fun onImageSelected(uri: Uri?) {
        Log.d(TAG, "Image selected with URI: $uri")
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun onTextInputChanged(newText: String) {
        Log.d(TAG, "Text input changed: $newText")
        _uiState.update { it.copy(textInput = newText, showConfirmationFailureMessage = false) }
    }

    fun onSendText() {
        val currentState = _uiState.value
        val currentText = currentState.textInput
        val imageUri = currentState.selectedImageUri

        Log.d(TAG, "onSendText called with text: '$currentText' and image URI: $imageUri")

        if (currentText.isBlank() && imageUri == null) {
            Log.w(TAG, "onSendText called with no text or image, ignoring.")
            return
        }

        val (systemPrompt, newFlowState, newProblemStatement) = when (currentState.flowState) {
            TutorialFlowState.INITIAL -> {
                Log.d(TAG, "onSendText in INITIAL state. Transitioning to INTERPRETING.")
                Triple(systemPromptInterpret, TutorialFlowState.INTERPRETING, currentText)
            }
            TutorialFlowState.AWAITING_CONFIRMATION, TutorialFlowState.CHATTING -> {
                Log.d(TAG, "onSendText in CHATTING state.")
                val history = mutableListOf<String>()
                history.add("Tutor found problem statement: ${currentState.initialProblemStatement}")
                val lastTutorMessage = currentState.tutorMessage
                if (lastTutorMessage != null) {
                    val lastTurn = "Tutor: $lastTutorMessage\nUser: $currentText"
                    history.add(lastTurn)
                }

                val socraticPrompt = systemPromptSocratic(history.joinToString("\n\n---\n\n"), subject = currentState.subject)
                Triple(socraticPrompt, TutorialFlowState.CHATTING, currentState.initialProblemStatement)
            }
            else -> {
                Log.w(TAG, "onSendText called in unexpected state: ${currentState.flowState}")
                return
            }
        }

        val isInterpreting = newFlowState == TutorialFlowState.INTERPRETING

        _uiState.update { it.copy(
            textInput = "",
            flowState = newFlowState,
            initialProblemStatement = newProblemStatement,
            tutorMessage = if (isInterpreting) "Estoy leyendo el problema..." else null,
            showConfirmationFailureMessage = false,
            selectedImageUri = null // Clear image after sending
        ).also {
            Log.d(TAG, "State updated for sending text. New flow state: ${it.flowState}")
        }}

        viewModelScope.launch {
            val fullPrompt = "$systemPrompt\n\nHere is the student's message:\n$currentText"
            val tokenCount = LlmHelper.sizeInTokens(fullPrompt)
            Log.d(TAG, "LLM Prompt ($tokenCount tokens): $fullPrompt")
            var fullResponse = ""

            val bitmaps = imageUri?.let { uri ->
                try {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    listOf(android.graphics.BitmapFactory.decodeStream(inputStream))
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading bitmap from URI: $uri", e)
                    emptyList()
                }
            } ?: emptyList()

            LlmHelper.runInference(
                input = fullPrompt,
                images = bitmaps,
                resultListener = { partialResult, done ->
                    fullResponse += partialResult
                    Log.d(TAG, "Partial result: $fullResponse")
                    if (done) {
                        Log.d(TAG, "LLM inference finished. Full response received.")
                        CoroutineScope(Dispatchers.Main).launch {
                            processLlmResponse(fullResponse)
                        }
                    }
                }
            )
        }
    }

    fun onConfirmationAccept() {
        Log.d(TAG, "Confirmation accepted by user.")
        val problemStatementFromTutor = _uiState.value.tutorMessage ?: _uiState.value.initialProblemStatement
        _uiState.update {
            it.copy(
                flowState = TutorialFlowState.CHATTING,
                tutorMessage = null, // Clear interpretation message
                initialProblemStatement = problemStatementFromTutor
            ).also {
                Log.d(TAG, "State updated for confirmation accept. New flow state: ${it.flowState}")
            }
        }

        // Kick off the Socratic dialogue
        viewModelScope.launch {
            val socraticPrompt = systemPromptSocratic("Tutor found problem statement: $problemStatementFromTutor", subject = _uiState.value.subject) + "\n\nNow, begin the conversation with a guiding question."
            val tokenCount = LlmHelper.sizeInTokens(socraticPrompt)
            Log.d(TAG, "LLM Prompt ($tokenCount tokens): $socraticPrompt")
            var fullResponse = ""
            LlmHelper.runInference(
                input = socraticPrompt,
                resultListener = { partialResult, done ->
                    fullResponse += partialResult
                    if (done) {
                        Log.d(TAG, "Socratic LLM inference finished. Full response received.")
                        CoroutineScope(Dispatchers.Main).launch {
                            processLlmResponse(fullResponse)
                        }
                    }
                }
            )
        }
    }

    fun onConfirmationReject() {
        Log.d(TAG, "Confirmation rejected by user. Resetting state.")
        _uiState.update {
            it.copy(
                flowState = TutorialFlowState.INITIAL,
                tutorMessage = null,
                subject = "",
                initialProblemStatement = "",
                showConfirmationFailureMessage = true
            ).also {
                Log.d(TAG, "State reset after rejection.")
            }
        }
    }
    
    /**
     * Extracts the subject/topic from the LLM interpretation response
     */
    private fun extractSubjectFromResponse(tutorMessage: String): String {
        return try {
            // Look for subject patterns in the tutor message
            val subjectPatterns = listOf(
                "\"subject\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
                "\"topic\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
                "\"problem_type\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
                "subject:\\s*([^,\\n}]+)".toRegex(),
                "topic:\\s*([^,\\n}]+)".toRegex(),
                "problem_type:\\s*([^,\\n}]+)".toRegex()
            )
            
            for (pattern in subjectPatterns) {
                val match = pattern.find(tutorMessage)
                if (match != null) {
                    val subject = match.groupValues[1].trim().replace("\"", "")
                    Log.d(TAG, "Extracted subject: '$subject' from tutor message")
                    return subject
                }
            }
            
            // Fallback: try to infer from common math keywords
            val mathKeywords = mapOf(
                "álgebra" to "álgebra",
                "geometría" to "geometría", 
                "aritmética" to "aritmética",
                "cálculo" to "cálculo",
                "trigonometría" to "trigonometría",
                "estadística" to "estadística",
                "probabilidad" to "probabilidad",
                "fracciones" to "aritmética",
                "ecuaciones" to "álgebra",
                "triángulo" to "geometría",
                "círculo" to "geometría",
                "derivada" to "cálculo",
                "integral" to "cálculo"
            )
            
            val lowerMessage = tutorMessage.lowercase()
            for ((keyword, subject) in mathKeywords) {
                if (lowerMessage.contains(keyword)) {
                    Log.d(TAG, "Inferred subject '$subject' from keyword '$keyword'")
                    return subject
                }
            }
            
            Log.d(TAG, "Could not extract subject from tutor message, defaulting to 'matemáticas'")
            "matemáticas" // Default subject
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting subject from response", e)
            "matemáticas" // Default fallback
        }
    }
}

class TutorialViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TutorialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TutorialViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 