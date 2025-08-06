package com.jjordanoc.yachai.ui.screens.whiteboard

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
import com.jjordanoc.yachai.ui.screens.whiteboard.model.MultiStepResponse
import com.jjordanoc.yachai.ui.screens.whiteboard.model.ExplanationStep
import com.jjordanoc.yachai.ui.screens.whiteboard.model.WhiteboardItem
import com.jjordanoc.yachai.ui.screens.whiteboard.animations.MathAnimation
import androidx.lifecycle.ViewModelProvider
import com.jjordanoc.yachai.llm.data.systemPrompt


// Removed TutorialFlowState enum - no longer needed since we only have one state

data class ChatHistoryEntry(
    val tutorMessage: String,
    val userMessage: String, // Add user input to track the full conversation
    val subject: String,
    val numberLine: WhiteboardItem.AnimatedNumberLine? = null,
    val expression: String? = null,
    val rectangle: WhiteboardItem.AnimatedRectangle? = null,
    val grid: WhiteboardItem.AnimatedGrid? = null,
    // Data visualization history
    val dataTable: WhiteboardItem.DataTable? = null,
    val tallyChart: WhiteboardItem.TallyChart? = null,
    val barChart: WhiteboardItem.BarChart? = null,
    val pieChart: WhiteboardItem.PieChart? = null,
    val dotPlot: WhiteboardItem.DotPlot? = null,
    val dataSummary: WhiteboardItem.DataSummary? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class TutorialState(
    val textInput: String = "",
    val selectedImageUri: Uri? = null,
    val tutorMessage: String? = null,
    val subject: String = "",
    val isModelLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val showConfirmationFailureMessage: Boolean = false,
    val initialProblemStatement: String = "",
    val isAlpacaSpeaking: Boolean = false,
    val isReadyForNextStep: Boolean = false, // True when alpaca finished speaking and user can proceed
    // New animation system - list of active animations
    val activeAnimations: List<MathAnimation> = emptyList(),
    val animationTrigger: Long = 0L, // Used to trigger animation recomposition
    // Legacy state for backward compatibility (will be removed)
    val currentNumberLine: WhiteboardItem.AnimatedNumberLine? = null,
    val currentExpression: String? = null,
    val currentRectangle: WhiteboardItem.AnimatedRectangle? = null,
    val currentGrid: WhiteboardItem.AnimatedGrid? = null,
    // Data visualization state
    val currentDataTable: WhiteboardItem.DataTable? = null,
    val currentTallyChart: WhiteboardItem.TallyChart? = null,
    val currentBarChart: WhiteboardItem.BarChart? = null,
    val currentPieChart: WhiteboardItem.PieChart? = null,
    val currentDotPlot: WhiteboardItem.DotPlot? = null,
    val currentDataSummary: WhiteboardItem.DataSummary? = null,
    // Chat history functionality
    val chatHistory: List<ChatHistoryEntry> = emptyList(),
    val currentHistoryIndex: Int = -1, // -1 means showing current/live content
    val isViewingHistory: Boolean = false,
    // Step sequence functionality
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val pendingSteps: List<ExplanationStep> = emptyList(),
    val isInStepSequence: Boolean = false,
    val stepTimer: Long = 0L
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
                LlmHelper.switchDataSource(LlmHelper.DataSourceType.MOCK, context, modelConfig)
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
    
    /**
     * Fallback method when TTS fails - manually trigger alpaca finished speaking
     */
    fun ttsFailed() {
        Log.w(TAG, "TTS failed - triggering fallback")
        alpacaFinishedSpeaking()
    }

    fun processLlmResponse(jsonString: String) {
        Log.d(TAG, "Processing LLM response: $jsonString")

        if (jsonString.startsWith("Error:")) {
            Log.e(TAG, "Received an error from LlmDataSource: $jsonString")
            _uiState.update { 
                it.copy(
                    tutorMessage = jsonString,
                    isAlpacaSpeaking = true, // Show error message with speaking animation
                    isProcessing = false // Set processing to false on error
                )
            }
            return
        }

        try {
            val cleanJsonString = cleanJsonResponse(jsonString)
            val currentState = _uiState.value

            // Parse as multi-step response
            val steps = json.decodeFromString<MultiStepResponse>(cleanJsonString)

            Log.d(TAG, "Parsed ${steps.size} tutorial steps")
            
            // Start sequential step processing
            startStepSequence(steps, currentState)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LLM response: $jsonString", e)
            _uiState.update { 
                it.copy(
                    isProcessing = false, // Set processing to false on parsing error
                    isAlpacaSpeaking = false // Stop alpaca animation on error
                )
            }
        }
    }
    
    private fun cleanJsonResponse(jsonString: String): String {
        val startIndex = if (jsonString.contains('[')) jsonString.indexOf('[') else jsonString.indexOf('{')
        val endIndex = if (jsonString.contains(']')) jsonString.lastIndexOf(']') else jsonString.lastIndexOf('}')
        
        return if (startIndex != -1 && endIndex > startIndex) {
                jsonString.substring(startIndex, endIndex + 1)
            } else {
            Log.w(TAG, "Could not find valid JSON in response. Trying to parse as is.")
                jsonString
        }
    }

    private fun startStepSequence(steps: List<ExplanationStep>, initialState: TutorialState) {
        if (steps.isEmpty()) {
            Log.w(TAG, "No steps to process")
            return
        }

        Log.d(TAG, "Starting step sequence with ${steps.size} steps")
        
        // Extract subject from the first step
        val extractedSubject = initialState.subject

        // Initialize step sequence state
        _uiState.update { state ->
            state.copy(
                pendingSteps = steps,
                currentStepIndex = 0,
                totalSteps = steps.size,
                isInStepSequence = true,
                isProcessing = false,
                isReadyForNextStep = false, // Start with button disabled
                subject = extractedSubject,
                currentHistoryIndex = -1,
                isViewingHistory = false,
                selectedImageUri = null // Clear image now that processing is complete
            )
        }

        // Process the first step immediately
        processCurrentStep()
    }

    private fun processCurrentStep() {
        val currentState = _uiState.value

        if (!currentState.isInStepSequence || 
            currentState.currentStepIndex >= currentState.pendingSteps.size) {
            Log.d(TAG, "No more steps to process or not in step sequence")
            completeStepSequence()
            return
        }

        val currentStep = currentState.pendingSteps[currentState.currentStepIndex]
        Log.d(TAG, "Processing step ${currentState.currentStepIndex + 1}/${currentState.totalSteps}: ${currentStep.tutorMessage}")

        // Clear previous animations and build the complete step snapshot
        var stepAnimations = emptyList<MathAnimation>()
        
        // Process each animation in the step's list
        currentStep.animations.forEachIndexed { index, animationCommand ->
            val isLastAnimation = index == currentStep.animations.size - 1
            
            // Process the animation command
            val newAnimations = processAnimationCommandNew(
                animationCommand,
                stepAnimations // Use the accumulated animations from this step
            )
            
            stepAnimations = newAnimations
            
            // Only trigger animation for the last/newest animation in the step
            if (isLastAnimation) {
                Log.d(TAG, "Processing final animation in step: ${animationCommand.command}")
            } else {
                Log.d(TAG, "Processing animation ${index + 1}/${currentStep.animations.size}: ${animationCommand.command}")
            }
        }

        val animationTrigger = System.currentTimeMillis()

        // Update state with current step results
        _uiState.update { state ->
            state.copy(
                tutorMessage = currentStep.tutorMessage,
                isAlpacaSpeaking = true,
                isReadyForNextStep = false, // Disable next step button until alpaca finishes speaking
                activeAnimations = stepAnimations,
                animationTrigger = animationTrigger
            )
        }

        Log.d(TAG, "Step ${currentState.currentStepIndex + 1} processed successfully with ${stepAnimations.size} animations")
    }

    private fun advanceToNextStep() {
        val currentState = _uiState.value
        
        if (!currentState.isInStepSequence) {
            Log.w(TAG, "Not in step sequence, cannot advance")
            return
        }

        val nextIndex = currentState.currentStepIndex + 1
        
        if (nextIndex >= currentState.totalSteps) {
            Log.d(TAG, "Reached end of step sequence")
            completeStepSequence()
            return
        }

        // Move to next step
        _uiState.update { state ->
            state.copy(currentStepIndex = nextIndex)
        }

        Log.d(TAG, "Advanced to step ${nextIndex + 1}/${currentState.totalSteps}")
        
        // Process the next step
        processCurrentStep()
    }

    private fun completeStepSequence() {
        Log.d(TAG, "Completing step sequence")
        
        val currentState = _uiState.value
        
        // Combine all step messages for chat history
        val allMessages = currentState.pendingSteps.map { it.tutorMessage }
        val combinedMessage = allMessages.joinToString(" ")
        
        // Add to chat history with final state
        val historyEntry = if (combinedMessage.isNotBlank()) {
            ChatHistoryEntry(
                tutorMessage = combinedMessage,
                userMessage = currentState.textInput,
                subject = currentState.subject,
                numberLine = currentState.currentNumberLine,
                expression = currentState.currentExpression,
                rectangle = currentState.currentRectangle,
                grid = currentState.currentGrid,
                dataTable = currentState.currentDataTable,
                tallyChart = currentState.currentTallyChart,
                barChart = currentState.currentBarChart,
                pieChart = currentState.currentPieChart,
                dotPlot = currentState.currentDotPlot,
                dataSummary = currentState.currentDataSummary
            )
        } else null

        // Reset step sequence state
        _uiState.update { state ->
            state.copy(
                isInStepSequence = false,
                currentStepIndex = 0,
                totalSteps = 0,
                pendingSteps = emptyList(),
                stepTimer = 0L,
                chatHistory = if (historyEntry != null) state.chatHistory + historyEntry else state.chatHistory
            )
        }

        Log.d(TAG, "Step sequence completed. Added to chat history.")
    }

    fun alpacaFinishedSpeaking() {
        val currentState = _uiState.value
        
        // Only enable next step if we're in a step sequence and not at the last step
        if (currentState.isInStepSequence && currentState.currentStepIndex < currentState.totalSteps - 1) {
            _uiState.update { it.copy(isReadyForNextStep = true, isAlpacaSpeaking = false) }
            Log.d(TAG, "Alpaca finished speaking - next step button enabled")
        } else {
            _uiState.update { it.copy(isAlpacaSpeaking = false) }
            Log.d(TAG, "Alpaca finished speaking - no more steps available")
        }
    }

    fun nextStepButtonHandler() {
        val currentState = _uiState.value
        
        if (!currentState.isInStepSequence || !currentState.isReadyForNextStep) {
            Log.d(TAG, "Cannot proceed to next step - not ready")
            return
        }
        
        if (currentState.currentStepIndex >= currentState.totalSteps - 1) {
            Log.d(TAG, "Cannot proceed - already at last step")
            return
        }
        
        // Reset ready state and proceed to next step
        _uiState.update { it.copy(isReadyForNextStep = false) }
        advanceToNextStep()
        Log.d(TAG, "Manually proceeding to next step")
    }

    fun repeatCurrentStep() {
        val currentState = _uiState.value
        
        // Can repeat if we have a current tutor message
        if (currentState.tutorMessage.isNullOrBlank()) {
            Log.d(TAG, "Cannot repeat - no current message")
            return
        }
        
        Log.d(TAG, "Repeating current step: ${currentState.tutorMessage}")
        
        // Trigger animation redraw and start alpaca speaking again
        val animationTrigger = System.currentTimeMillis()
        
        _uiState.update { state ->
            state.copy(
                isAlpacaSpeaking = true,
                isReadyForNextStep = false, // Disable next step until repeat finishes
                animationTrigger = animationTrigger // Trigger animation redraw
            )
        }
        
        Log.d(TAG, "Current step repeat initiated - animations will redraw and TTS will restart")
    }


    /**
     * Process animation command and return a list of active animations
     * This is the new animation system that replaces the tuple approach
     */
    private fun processAnimationCommandNew(
        command: com.jjordanoc.yachai.ui.screens.whiteboard.model.AnimationCommand,
        currentAnimations: List<MathAnimation>
    ): List<MathAnimation> {
        val newAnimations = currentAnimations.toMutableList()
        
        Log.d(TAG, "Processing animation command: ${command.command} with args: ${command.args}")
        
        // Try to create animation from command
        val animation = MathAnimation.fromCommand(command)
        
        if (animation != null) {
            // Add the new animation
            newAnimations.add(animation)
            Log.d(TAG, "Created animation: ${animation::class.simpleName}")
        } else {
            Log.w(TAG, "Failed to create animation for command: ${command.command}")
        }
        
        return newAnimations
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
        val systemPromptStr = systemPrompt()

        Log.d(TAG, "onSendText called with text: '$currentText' and image URI: $imageUri")

        if (currentText.isBlank() && imageUri == null) {
            Log.w(TAG, "onSendText called with no text or image, ignoring.")
            return
        }

        _uiState.update { it.copy(
            textInput = "",
            initialProblemStatement = currentText,
            tutorMessage = null, // No loading message needed since we go straight to chatting
            isProcessing = true, // Set processing to true when starting to process
            isAlpacaSpeaking = true, // Start alpaca animation to show thinking
            showConfirmationFailureMessage = false
            // Keep selectedImageUri so it can be displayed in loading screen
        ).also {
            Log.d(TAG, "State updated for sending text.")
        }}

        viewModelScope.launch {
            val fullPrompt = "$systemPromptStr\n\nHere is the student's message:\n$currentText"
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
    
    fun cancelLlmInference() {
        Log.d(TAG, "Cancelling LLM inference")
        // Stop processing and reset state
        _uiState.update { it.copy(
            isProcessing = false,
            isAlpacaSpeaking = false,
            tutorMessage = null
        ) }
        // Note: Actual LLM cancellation would need to be implemented in LlmHelper
        // For now, we just reset the UI state
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