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
import com.jjordanoc.yachai.ui.screens.whiteboard.model.LlmResponse
import com.jjordanoc.yachai.ui.screens.whiteboard.model.MultiStepResponse
import com.jjordanoc.yachai.ui.screens.whiteboard.model.TutorialStep
import com.jjordanoc.yachai.ui.screens.whiteboard.model.WhiteboardItem
import com.jjordanoc.yachai.ui.screens.whiteboard.model.RectanglePhase
import com.jjordanoc.yachai.ui.screens.whiteboard.model.GridPhase
import androidx.lifecycle.ViewModelProvider

data class Tuple10<A, B, C, D, E, F, G, H, I, J>(
    val first: A, val second: B, val third: C, val fourth: D, val fifth: E,
    val sixth: F, val seventh: G, val eighth: H, val ninth: I, val tenth: J
)

enum class TutorialFlowState {
    CHATTING
}

data class ChatHistoryEntry(
    val tutorMessage: String,
    val userMessage: String, // Add user input to track the full conversation
    val subject: String,
    val flowState: TutorialFlowState,
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
    val flowState: TutorialFlowState = TutorialFlowState.CHATTING,
    val isModelLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val showConfirmationFailureMessage: Boolean = false,
    val initialProblemStatement: String = "",
    val isAlpacaSpeaking: Boolean = false,
    val isReadyForNextStep: Boolean = false, // True when alpaca finished speaking and user can proceed
    val currentNumberLine: WhiteboardItem.AnimatedNumberLine? = null,
    val currentExpression: String? = null,
    val currentRectangle: WhiteboardItem.AnimatedRectangle? = null,
    val currentGrid: WhiteboardItem.AnimatedGrid? = null,
    val animationTrigger: Long = 0L, // Used to trigger animation recomposition
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
    val pendingSteps: List<TutorialStep> = emptyList(),
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
                LlmHelper.switchDataSource(LlmHelper.DataSourceType.MEDIAPIPE, context, modelConfig)
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
                    isAlpacaSpeaking = true, // Show error message with speaking animation
                    isProcessing = false // Set processing to false on error
                )
            }
            return
        }

        try {
            val cleanJsonString = cleanJsonResponse(jsonString)
            val currentState = _uiState.value

            // Try to parse as multi-step response first
            val steps = try {
                json.decodeFromString<MultiStepResponse>(cleanJsonString)
            } catch (e: Exception) {
                Log.d(TAG, "Failed to parse as multi-step response, trying legacy format: ${e.message}")
                // Fall back to legacy single response format
                val legacyResponse = json.decodeFromString<LlmResponse>(cleanJsonString)
                listOf(TutorialStep(
                    tutorMessage = legacyResponse.tutorMessage ?: "",
                    animation = legacyResponse.animation.firstOrNull() ?: return
                ))
            }

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

    private fun startStepSequence(steps: List<TutorialStep>, initialState: TutorialState) {
        if (steps.isEmpty()) {
            Log.w(TAG, "No steps to process")
            return
        }

        Log.d(TAG, "Starting step sequence with ${steps.size} steps")
        
        // Extract subject from the first step
        val extractedSubject = if (initialState.subject.isBlank()) {
            extractSubjectFromResponse(steps.firstOrNull()?.tutorMessage ?: "")
        } else {
            initialState.subject
        }

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
                isViewingHistory = false
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

        // Handle clear_previous logic
        var clearedState = currentState
        if (currentStep.animation.clearPrevious) {
            Log.d(TAG, "Clearing previous animations due to clear_previous=true")
            clearedState = currentState.copy(
                currentNumberLine = null,
                currentExpression = null,
                currentRectangle = null,
                currentGrid = null,
                currentDataTable = null,
                currentTallyChart = null,
                currentBarChart = null,
                currentPieChart = null,
                currentDotPlot = null,
                currentDataSummary = null
            )
        }

        // Process the animation command for current step
        val animationResult = processAnimationCommand(
            currentStep.animation,
            clearedState.currentNumberLine,
            clearedState.currentExpression,
            clearedState.currentRectangle,
            clearedState.currentGrid,
            clearedState.currentDataTable,
            clearedState.currentTallyChart,
            clearedState.currentBarChart,
            clearedState.currentPieChart,
            clearedState.currentDotPlot,
            clearedState.currentDataSummary
        )

        val animationTrigger = System.currentTimeMillis()

        // Update state with current step results
        _uiState.update { state ->
            state.copy(
                tutorMessage = currentStep.tutorMessage,
                isAlpacaSpeaking = true,
                isReadyForNextStep = false, // Disable next step button until alpaca finishes speaking
                currentNumberLine = animationResult.first,
                currentExpression = animationResult.second,
                currentRectangle = animationResult.third,
                currentGrid = animationResult.fourth,
                currentDataTable = animationResult.fifth,
                currentTallyChart = animationResult.sixth,
                currentBarChart = animationResult.seventh,
                currentPieChart = animationResult.eighth,
                currentDotPlot = animationResult.ninth,
                currentDataSummary = animationResult.tenth,
                animationTrigger = animationTrigger
            )
        }

        Log.d(TAG, "Step ${currentState.currentStepIndex + 1} processed successfully")
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
                flowState = currentState.flowState,
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

    fun proceedToNextStep() {
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

    @Deprecated("Use alpacaFinishedSpeaking() for manual progression")
    fun scheduleNextStep(delayMs: Long = 5000L) {
        // Keep for backward compatibility but prefer manual progression
        alpacaFinishedSpeaking()
    }

    // Manual step control functions
    fun skipToNextStep() {
        val currentState = _uiState.value
        if (currentState.isInStepSequence) {
            Log.d(TAG, "Manually skipping to next step")
            advanceToNextStep()
        }
    }

    fun pauseStepSequence() {
        _uiState.update { state ->
            state.copy(stepTimer = 0L)
        }
        Log.d(TAG, "Step sequence paused")
    }

    fun resumeStepSequence() {
        scheduleNextStep()
        Log.d(TAG, "Step sequence resumed")
    }

    private fun processAnimationCommand(
        command: com.jjordanoc.yachai.ui.screens.whiteboard.model.AnimationCommand,
        currentNumberLine: WhiteboardItem.AnimatedNumberLine?,
        currentExpression: String?,
        currentRectangle: WhiteboardItem.AnimatedRectangle?,
        currentGrid: WhiteboardItem.AnimatedGrid?,
        currentDataTable: WhiteboardItem.DataTable?,
        currentTallyChart: WhiteboardItem.TallyChart?,
        currentBarChart: WhiteboardItem.BarChart?,
        currentPieChart: WhiteboardItem.PieChart?,
        currentDotPlot: WhiteboardItem.DotPlot?,
        currentDataSummary: WhiteboardItem.DataSummary?
    ): Tuple10<WhiteboardItem.AnimatedNumberLine?, String?, WhiteboardItem.AnimatedRectangle?, WhiteboardItem.AnimatedGrid?, WhiteboardItem.DataTable?, WhiteboardItem.TallyChart?, WhiteboardItem.BarChart?, WhiteboardItem.PieChart?, WhiteboardItem.DotPlot?, WhiteboardItem.DataSummary?> {
        
        var newNumberLine = currentNumberLine
        var newExpression = currentExpression
        var newRectangle = currentRectangle
        var newGrid = currentGrid
        var newDataTable = currentDataTable
        var newTallyChart = currentTallyChart
        var newBarChart = currentBarChart
        var newPieChart = currentPieChart
        var newDotPlot = currentDotPlot
        var newDataSummary = currentDataSummary

        when (command.command) {
            "drawNumberLine" -> {
                Log.d(TAG, "drawNumberLine command found with args: ${command.args}")
                val range = command.args.range
                val marks = command.args.marks
                val highlight = command.args.highlight
                
                if (range != null && marks != null && highlight != null) {
                    newNumberLine = WhiteboardItem.AnimatedNumberLine(
                        range = range,
                        marks = marks,
                        highlight = highlight
                    )
                    Log.d(TAG, "Created number line: range=$range, marks=$marks, highlight=$highlight")
                } else {
                    Log.w(TAG, "Invalid arguments for drawNumberLine: ${command.args}")
                }
            }

            "updateNumberLine" -> {
                command.args.highlight?.let { highlight ->
                    Log.d(TAG, "updateNumberLine command found with highlight: $highlight")
                    if (newNumberLine != null) {
                        newNumberLine = newNumberLine.copy(highlight = highlight)
                        Log.d(TAG, "Updated number line highlight: $highlight")
                    }
                }
            }

                    "appendExpression" -> {
                        command.args.expression?.let { expression ->
                            Log.d(TAG, "appendExpression command found with expression: $expression")
                            newExpression = expression
                        }
                    }

                    "drawRectangle" -> {
                        Log.d(TAG, "drawRectangle command found with args: ${command.args}")
                
                // Handle new string-based base/height parameters
                val baseStr = command.args.base
                val heightStr = command.args.height
                
                // Also support legacy numeric parameters
                val lengthNum = command.args.length
                val widthNum = command.args.width
                
                // Parse string values to integers
                val length = try {
                    baseStr?.toInt() ?: lengthNum
                } catch (e: NumberFormatException) {
                    lengthNum
                }
                
                val width = try {
                    heightStr?.toInt() ?: widthNum
                } catch (e: NumberFormatException) {
                    widthNum
                }
                
                val lengthLabel = command.args.lengthLabel ?: "base"
                val widthLabel = command.args.widthLabel ?: "altura"
                        
                        if (length != null && width != null && length > 0 && width > 0) {
                            newRectangle = WhiteboardItem.AnimatedRectangle(
                                length = length,
                                width = width,
                                lengthLabel = lengthLabel,
                                widthLabel = widthLabel,
                                animationPhase = RectanglePhase.SETUP
                            )
                            Log.d(TAG, "Created rectangle: length=$length, width=$width")
                        } else {
                    Log.w(TAG, "Invalid arguments for drawRectangle: base=$baseStr, height=$heightStr")
                }
            }

            "drawGrid" -> {
                Log.d(TAG, "drawGrid command found with args: ${command.args}")
                
                // Parse grid parameters - following TutorPrompts.kt structure: length, width, unit
                val lengthStr = command.args.base // "length" parameter 
                val widthStr = command.args.height // "width" parameter
                val unit = command.args.unit ?: "1"
                
                // Also try numeric versions and alternate field names
                val lengthNum = command.args.length ?: command.args.width
                val widthNum = command.args.width ?: command.args.length
                
                val gridLength = try {
                    lengthStr?.toInt() ?: lengthNum
                } catch (e: NumberFormatException) {
                    lengthNum
                }
                
                val gridWidth = try {
                    widthStr?.toInt() ?: widthNum  
                } catch (e: NumberFormatException) {
                    widthNum
                }
                
                if (gridLength != null && gridWidth != null && gridLength > 0 && gridWidth > 0) {
                    // Create AnimatedGrid that will overlay on top of existing rectangle
                    newGrid = WhiteboardItem.AnimatedGrid(
                        length = gridLength,
                        width = gridWidth,
                        unit = unit,
                        animationPhase = GridPhase.SETUP, // Start with setup phase
                        lengthLabel = "largo",
                        widthLabel = "ancho"
                    )
                    Log.d(TAG, "Created animated grid: ${gridLength}x${gridWidth}, unit=$unit")
                } else {
                    Log.w(TAG, "Invalid arguments for drawGrid: length=$lengthStr ($lengthNum), width=$widthStr ($widthNum)")
                }
            }

            "updateGrid" -> {
                Log.d(TAG, "updateGrid command found")
                if (newGrid != null) {
                    // Progress the grid animation phase
                    val nextPhase = when (newGrid.animationPhase) {
                        GridPhase.SETUP -> GridPhase.GRID_LINES
                        GridPhase.GRID_LINES -> GridPhase.FILLING_UNITS
                        GridPhase.FILLING_UNITS -> {
                            // Advance filling by one unit square with smooth progress
                            val nextColumn = if (newGrid.currentColumn >= newGrid.length - 1) {
                                0
                            } else {
                                newGrid.currentColumn + 1
                            }
                            val nextRow = if (newGrid.currentColumn >= newGrid.length - 1) {
                                newGrid.currentRow + 1
                            } else {
                                newGrid.currentRow
                            }
                            
                            newGrid = newGrid.copy(
                                currentRow = nextRow,
                                currentColumn = nextColumn,
                                fillProgress = 0f // Reset progress for next unit
                            )
                            GridPhase.FILLING_UNITS
                        }
                    }
                    
                    if (newGrid.animationPhase != GridPhase.FILLING_UNITS) {
                        newGrid = newGrid.copy(animationPhase = nextPhase)
                    }
                    
                    Log.d(TAG, "Updated grid phase: $nextPhase, row: ${newGrid.currentRow}, col: ${newGrid.currentColumn}")
                }
            }

            "highlightSide" -> {
                Log.d(TAG, "highlightSide command found with args: ${command.args}")
                val segment = command.args.segment
                val label = command.args.label
                
                if (segment != null && label != null && newRectangle != null) {
                    // Update the rectangle labels based on which side is being highlighted
                    val updatedRectangle = when (segment.lowercase()) {
                        "base", "length", "horizontal" -> {
                            newRectangle.copy(lengthLabel = label)
                        }
                        "height", "width", "vertical", "altura" -> {
                            newRectangle.copy(widthLabel = label)
                        }
                        else -> {
                            Log.w(TAG, "Unknown segment for highlightSide: $segment")
                            newRectangle
                        }
                    }
                    newRectangle = updatedRectangle
                    Log.d(TAG, "Highlighted side $segment with label: $label")
                } else {
                    Log.w(TAG, "Invalid arguments for highlightSide: segment=$segment, label=$label")
                }
            }

                    "updateRectangle" -> {
                        Log.d(TAG, "updateRectangle command found")
                        if (newRectangle != null) {
                            val nextPhase = when (newRectangle.animationPhase) {
                                RectanglePhase.SETUP -> RectanglePhase.VERTICAL_LINES
                                RectanglePhase.VERTICAL_LINES -> RectanglePhase.FILLING_ROWS
                                RectanglePhase.FILLING_ROWS -> {
                                    val nextRow = if (newRectangle.currentColumn >= newRectangle.length - 1) {
                                        newRectangle.currentRow + 1
                                    } else {
                                        newRectangle.currentRow
                                    }
                                    val nextColumn = if (newRectangle.currentColumn >= newRectangle.length - 1) {
                                        0
                                    } else {
                                        newRectangle.currentColumn + 1
                                    }
                                    
                                    newRectangle = newRectangle.copy(
                                        currentRow = nextRow,
                                        currentColumn = nextColumn
                                    )
                                    RectanglePhase.FILLING_ROWS
                                }
                            }
                            
                            if (newRectangle.animationPhase != RectanglePhase.FILLING_ROWS) {
                                newRectangle = newRectangle.copy(animationPhase = nextPhase)
                            }
                            
                    Log.d(TAG, "Updated rectangle phase: $nextPhase")
                        }
                    }
                    
                    // Data visualization commands
                    "drawTable" -> {
                        command.args.headers?.let { headers ->
                            command.args.rows?.let { rows ->
                                Log.d(TAG, "drawTable command found with ${headers.size} headers and ${rows.size} rows")
                        newDataTable = WhiteboardItem.DataTable(headers = headers, rows = rows)
                            }
                        }
                    }

                    "drawTallyChart" -> {
                        command.args.categories?.let { categories ->
                            command.args.counts?.let { counts ->
                                Log.d(TAG, "drawTallyChart command found with ${categories.size} categories")
                        newTallyChart = WhiteboardItem.TallyChart(categories = categories, counts = counts)
                            }
                        }
                    }

                    "drawBarChart" -> {
                        command.args.labels?.let { labels ->
                            command.args.values?.let { values ->
                                Log.d(TAG, "drawBarChart command found with ${labels.size} bars")
                        newBarChart = WhiteboardItem.BarChart(labels = labels, values = values)
                            }
                        }
                    }

                    "drawPieChart" -> {
                        command.args.labels?.let { labels ->
                            command.args.values?.let { values ->
                                Log.d(TAG, "drawPieChart command found with ${labels.size} slices")
                        newPieChart = WhiteboardItem.PieChart(labels = labels, values = values)
                            }
                        }
                    }

                    "drawDotPlot" -> {
                        command.args.values?.let { values ->
                            val min = command.args.min ?: values.minOrNull() ?: 0
                            val max = command.args.max ?: values.maxOrNull() ?: 10
                            Log.d(TAG, "drawDotPlot command found with ${values.size} values")
                    newDotPlot = WhiteboardItem.DotPlot(values = values, min = min, max = max)
                }
            }

                    "highlightData" -> {
                        command.args.index?.let { index ->
                            when (command.args.type) {
                                "bar" -> newBarChart = newBarChart?.copy(highlightedIndex = index)
                                "slice" -> newPieChart = newPieChart?.copy(highlightedIndex = index)
                                "dot" -> newDotPlot = newDotPlot?.copy(highlightedIndices = listOf(index))
                            }
                            Log.d(TAG, "highlightData command: highlighted ${command.args.type} at index $index")
                        }
                    }

                    "appendDataSummary" -> {
                        command.args.summary?.let { summary ->
                            Log.d(TAG, "appendDataSummary command found: $summary")
                            newDataSummary = WhiteboardItem.DataSummary(
                                summary = summary,
                                meanValue = command.args.value,
                                rangeMin = command.args.min,
                                rangeMax = command.args.max
                            )
                        }
                    }

                    "drawMeanLine", "showDataRange" -> {
                        Log.d(TAG, "${command.command} command processed")
                    }

                    else -> {
                        Log.w(TAG, "Unknown animation command: ${command.command}")
            }
        }

        return Tuple10(newNumberLine, newExpression, newRectangle, newGrid, newDataTable, newTallyChart, newBarChart, newPieChart, newDotPlot, newDataSummary)
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

        // Always in CHATTING state now - ProblemInputScreen handles initial input
        val (systemPrompt, newFlowState, newProblemStatement) = if (currentState.initialProblemStatement.isBlank()) {
            Log.d(TAG, "onSendText with new problem statement. Starting tutorial.")
            // First time input - use as problem statement and start Socratic dialogue
            val socraticPrompt = systemPromptSocratic("") // Default subject initially
            Triple(socraticPrompt, TutorialFlowState.CHATTING, currentText)
        } else {
            Log.d(TAG, "onSendText in ongoing conversation.")
            // Ongoing conversation - build history and continue
            val fullConversationHistory = buildConversationHistory(currentState, currentText)
            val socraticPrompt = systemPromptSocratic(fullConversationHistory)
            Triple(socraticPrompt, TutorialFlowState.CHATTING, currentState.initialProblemStatement)
        }

        _uiState.update { it.copy(
            textInput = "",
            flowState = newFlowState,
            initialProblemStatement = newProblemStatement,
            tutorMessage = null, // No loading message needed since we go straight to chatting
            isProcessing = true, // Set processing to true when starting to process
            isAlpacaSpeaking = true, // Start alpaca animation to show thinking
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
    
    /**
     * Navigate to previous message in chat history
     */
    fun navigateToPreviousMessage() {
        val currentState = _uiState.value
        if (currentState.chatHistory.isNotEmpty()) {
            val newIndex = if (currentState.isViewingHistory) {
                maxOf(0, currentState.currentHistoryIndex - 1)
            } else {
                currentState.chatHistory.size - 1 // Start from most recent
            }
            
            if (newIndex >= 0 && newIndex < currentState.chatHistory.size) {
                val historyEntry = currentState.chatHistory[newIndex]
                _uiState.update { 
                    it.copy(
                        currentHistoryIndex = newIndex,
                        isViewingHistory = true,
                        // Show historical content
                        tutorMessage = historyEntry.tutorMessage,
                        subject = historyEntry.subject,
                        flowState = historyEntry.flowState, // Preserve historical flow state
                        currentNumberLine = historyEntry.numberLine,
                        currentExpression = historyEntry.expression,
                        currentRectangle = historyEntry.rectangle,
                        currentDataTable = historyEntry.dataTable,
                        currentTallyChart = historyEntry.tallyChart,
                        currentBarChart = historyEntry.barChart,
                        currentPieChart = historyEntry.pieChart,
                        currentDotPlot = historyEntry.dotPlot,
                        currentDataSummary = historyEntry.dataSummary,
                        isAlpacaSpeaking = true // Trigger alpaca speaking for historical message
                    )
                }
                Log.d(TAG, "Navigated to previous message: index $newIndex")
            }
        }
    }
    
    /**
     * Navigate to next message in chat history
     */
    fun navigateToNextMessage() {
        val currentState = _uiState.value
        if (currentState.isViewingHistory && currentState.chatHistory.isNotEmpty()) {
            val newIndex = currentState.currentHistoryIndex + 1
            
            if (newIndex < currentState.chatHistory.size) {
                val historyEntry = currentState.chatHistory[newIndex]
                _uiState.update { 
                    it.copy(
                        currentHistoryIndex = newIndex,
                        // Show historical content
                        tutorMessage = historyEntry.tutorMessage,
                        subject = historyEntry.subject,
                        flowState = historyEntry.flowState, // Preserve historical flow state
                        currentNumberLine = historyEntry.numberLine,
                        currentExpression = historyEntry.expression,
                        currentRectangle = historyEntry.rectangle,
                        currentDataTable = historyEntry.dataTable,
                        currentTallyChart = historyEntry.tallyChart,
                        currentBarChart = historyEntry.barChart,
                        currentPieChart = historyEntry.pieChart,
                        currentDotPlot = historyEntry.dotPlot,
                        currentDataSummary = historyEntry.dataSummary,
                        isAlpacaSpeaking = true // Trigger alpaca speaking for historical message
                    )
                }
                Log.d(TAG, "Navigated to next message: index $newIndex")
            } else {
                // Return to current/live content
                returnToCurrentMessage()
            }
        }
    }
    
    /**
     * Return to current/live message (exit history viewing)
     */
    fun returnToCurrentMessage() {
        _uiState.update { state ->
            // We need to restore the actual current state from the last real interaction
            val lastHistoryEntry = state.chatHistory.lastOrNull()
            state.copy(
                currentHistoryIndex = -1,
                isViewingHistory = false,
                tutorMessage = lastHistoryEntry?.tutorMessage,
                subject = lastHistoryEntry?.subject ?: state.subject,
                flowState = lastHistoryEntry?.flowState ?: TutorialFlowState.CHATTING, // Restore flow state
                currentNumberLine = lastHistoryEntry?.numberLine,
                currentExpression = lastHistoryEntry?.expression,
                isAlpacaSpeaking = true
            )
        }
        Log.d(TAG, "Returned to current message")
    }
    
    /**
     * Check if we can navigate to previous message
     */
    fun canNavigatePrevious(): Boolean {
        val currentState = _uiState.value
        return currentState.chatHistory.isNotEmpty() && 
               (!currentState.isViewingHistory || currentState.currentHistoryIndex > 0)
    }
    
    /**
     * Check if we can navigate to next message
     */
    fun canNavigateNext(): Boolean {
        val currentState = _uiState.value
        return currentState.isViewingHistory && 
               currentState.currentHistoryIndex < currentState.chatHistory.size - 1
    }
    
    /**
     * Build compact conversation history for LLM context (optimized for small models)
     */
    private fun buildConversationHistory(currentState: TutorialState, currentUserInput: String): String {
        val history = mutableListOf<String>()
        
        // Add problem statement (essential context)
        history.add("Problem: ${currentState.initialProblemStatement}")
        
        // Only include last 3-4 exchanges to keep token count low
        val recentHistory = currentState.chatHistory.takeLast(3)
        
        recentHistory.forEach { entry ->
            // Compact format: S=Student, T=Tutor
            if (entry.userMessage.isNotBlank()) {
                history.add("S: ${entry.userMessage}")
            }
            
            // Just the essential tutor message, no verbose descriptions
            var tutorMsg = "T: ${entry.tutorMessage}"
            
            // Add minimal visual context
            entry.numberLine?.let { 
                tutorMsg += " [showed numbers]"
            }
            entry.expression?.let { 
                tutorMsg += " [showed: ${it}]"
            }
            
            // Add compact visual context for data
            entry.dataTable?.let { tutorMsg += " [table]" }
            entry.tallyChart?.let { tutorMsg += " [tally]" }
            entry.barChart?.let { tutorMsg += " [bars]" }
            entry.pieChart?.let { tutorMsg += " [pie chart]" }
            entry.dotPlot?.let { tutorMsg += " [dots]" }
            entry.dataSummary?.let { tutorMsg += " [summary]" }
            
            history.add(tutorMsg)
        }
        
        // Add current input
        if (currentUserInput.isNotBlank()) {
            history.add("S: $currentUserInput")
        }
        
        val compactHistory = history.joinToString(" | ")
        Log.d(TAG, "Built compact history (${compactHistory.length} chars): $compactHistory")
        
        return compactHistory
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