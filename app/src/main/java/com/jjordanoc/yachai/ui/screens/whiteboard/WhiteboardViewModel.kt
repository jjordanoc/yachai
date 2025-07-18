package com.jjordanoc.yachai.ui.screens.whiteboard

import android.app.Application
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.jjordanoc.yachai.utils.TAG
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.net.Uri
import com.jjordanoc.yachai.llm.data.Models
import com.jjordanoc.yachai.llm.data.getLocalPath
import com.jjordanoc.yachai.utils.SettingsManager
import com.jjordanoc.yachai.llm.data.ModelConfig
import com.jjordanoc.yachai.llm.LlmHelper
import com.jjordanoc.yachai.ui.screens.whiteboard.model.AnimationCommand
import com.jjordanoc.yachai.ui.screens.whiteboard.model.InterpretResponse
import com.jjordanoc.yachai.ui.screens.whiteboard.model.LlmResponse
import com.jjordanoc.yachai.ui.screens.whiteboard.model.SideLengths
import com.jjordanoc.yachai.ui.screens.whiteboard.model.WhiteboardItem
import com.jjordanoc.yachai.ui.screens.whiteboard.model.WhiteboardState

private const val GRID_SIZE = 9

enum class WhiteboardFlowState {
    INITIAL,
    INTERPRETING,
    AWAITING_CONFIRMATION,
    SOCRATIC_TUTORING
}

class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WhiteboardState())
    val uiState: StateFlow<WhiteboardState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val viewModelScope = CoroutineScope(Dispatchers.IO)


    init {
        Log.d(TAG, "WhiteboardViewModel initialized.")
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
        Log.d(TAG, "WhiteboardViewModel cleared and LlmHelper cleaned up.")
    }

    private fun getNextGridPosition(state: WhiteboardState): Pair<Int, Int>? {
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                if (!state.gridItems.containsKey(row to col)) {
                    return row to col
                }
            }
        }
        return null // Grid is full
    }

    fun processLlmResponse(jsonString: String) {
        Log.d(TAG, "Processing LLM response: $jsonString")

        if (jsonString.startsWith("Error:")) {
            Log.e(TAG, "Received an error from LlmDataSource: $jsonString")
            _uiState.update { it.copy(tutorMessage = jsonString) }
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

            // Adapt parsing based on flow state
            val response: LlmResponse = if (currentState.flowState == WhiteboardFlowState.INTERPRETING) {
                val interpretResponse = json.decodeFromString<InterpretResponse>(cleanJsonString)
                LlmResponse(
                    tutorMessage = interpretResponse.tutorMessage,
                    hint = null,
                    animation = if (interpretResponse.command != null && interpretResponse.args != null) {
                        listOf(AnimationCommand(interpretResponse.command, interpretResponse.args))
                    } else {
                        emptyList()
                    }
                )
            } else {
                json.decodeFromString<LlmResponse>(cleanJsonString)
            }

            Log.d(TAG, "LLM response parsed successfully: $response")

            _uiState.update { state ->
                var newGridItems = state.gridItems.toMutableMap()

                for (command in response.animation) {
                    when (command.command) {
                        "drawRightTriangle" -> {
                            command.args.let { args ->
                                Log.d(TAG, "drawRightTriangle command found with args: $args")
                                val sideLengths = SideLengths(ac = args.ac, ab = args.ab, bc = args.bc)
                                val sideAB = sideLengths.ab?.toFloatOrNull() ?: 5f
                                val sideAC = sideLengths.ac?.toFloatOrNull() ?: 13f
                                val sideBC = sqrt(sideAC * sideAC - sideAB * sideAB)

                                val drawScale = 30f

                                val pointA = Offset(0f, -sideAB * drawScale)
                                val pointB = Offset.Zero
                                val pointC = Offset(sideBC * drawScale, 0f)

                                val triangle = WhiteboardItem.AnimatedTriangle(
                                    a = pointA,
                                    b = pointB,
                                    c = pointC,
                                    sideLengths = sideLengths
                                )
                                getNextGridPosition(state.copy(gridItems = newGridItems))?.let { pos ->
                                    newGridItems[pos] = triangle
                                } ?: Log.w(TAG, "Whiteboard grid is full, cannot draw triangle.")
                            }
                        }
                        "highlightSide" -> {
                            command.args.segment?.let { segment ->
                                Log.d(TAG, "highlightSide command found with segment: $segment")
                                newGridItems = newGridItems.mapValues { (_, item) ->
                                    if (item is WhiteboardItem.AnimatedTriangle) {
                                        item.copy(highlightedSides = (item.highlightedSides) + segment)
                                    } else {
                                        item
                                    }
                                }.toMutableMap()
                            }
                        }
                        "highlightAngle" -> {
                            command.args.point?.let { point ->
                                Log.d(TAG, "highlightAngle command found with point: $point")
                                newGridItems = newGridItems.mapValues { (_, item) ->
                                    if (item is WhiteboardItem.AnimatedTriangle) {
                                        item.copy(highlightedAngle = point)
                                    } else {
                                        item
                                    }
                                }.toMutableMap()
                            }
                        }
                        "appendExpression" -> {
                            command.args.expression?.let { expression ->
                                Log.d(TAG, "appendExpression command found with expression: $expression")
                                getNextGridPosition(state.copy(gridItems = newGridItems))?.let { pos ->
                                    newGridItems[pos] = WhiteboardItem.Expression(expression)
                                } ?: Log.w(TAG, "Whiteboard grid is full, cannot add expression.")
                            }
                        }
                        "drawNumberLine" -> {
                            command.args.let { args ->
                                if (args.range != null && args.marks != null && args.highlight != null) {
                                    Log.d(TAG, "drawNumberLine command found with args: $args")
                                    val numberLine = WhiteboardItem.AnimatedNumberLine(
                                        range = args.range,
                                        marks = args.marks,
                                        highlight = args.highlight
                                    )
                                    getNextGridPosition(state.copy(gridItems = newGridItems))?.let { pos ->
                                        newGridItems[pos] = numberLine
                                    } ?: Log.w(TAG, "Whiteboard grid is full, cannot draw number line.")
                                }
                            }
                        }
                        "updateNumberLine" -> {
                            command.args.highlight?.let { highlight ->
                                Log.d(TAG, "updateNumberLine command found with highlight: $highlight")
                                newGridItems = newGridItems.mapValues { (_, item) ->
                                    if (item is WhiteboardItem.AnimatedNumberLine) {
                                        item.copy(highlight = highlight)
                                    } else {
                                        item
                                    }
                                }.toMutableMap()
                            }
                        }
                        else -> {
                            Log.w(TAG, "Unknown animation command: ${command.command}")
                        }
                    }
                }

                val newFlowState = if (state.flowState == WhiteboardFlowState.INTERPRETING) {
                    WhiteboardFlowState.AWAITING_CONFIRMATION
                } else {
                    state.flowState
                }

                Log.d(TAG, "Updating flow state to $newFlowState")

                state.copy(
                    gridItems = newGridItems,
                    tutorMessage = response.tutorMessage,
                    hint = response.hint,
                    flowState = newFlowState
                ).also {
                    Log.d(TAG, "State updated with new tutor message.")
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
            WhiteboardFlowState.INITIAL -> {
                Log.d(TAG, "onSendText in INITIAL state. Transitioning to INTERPRETING.")
                Triple(systemPromptInterpret, WhiteboardFlowState.INTERPRETING, currentText)
            }
            WhiteboardFlowState.AWAITING_CONFIRMATION, WhiteboardFlowState.SOCRATIC_TUTORING -> {
                Log.d(TAG, "onSendText in SOCRATIC_TUTORING state.")
                val history = mutableListOf<String>()
                history.add("Tutor found problem statement: ${currentState.initialProblemStatement}")
                val lastTutorMessage = currentState.tutorMessage
                val lastHint = currentState.hint
                if (lastTutorMessage != null) {
                    var lastTurn = "Tutor: $lastTutorMessage"
                    if (lastHint != null) {
                       lastTurn += "\nHint: $lastHint"
                    }
                    lastTurn += "\nUser: $currentText"
                    history.add(lastTurn)
                }

                val problemType = currentState.tutorMessage?.substringAfter("problem_type\": \"", "")?.substringBefore("\"", "")
                val socraticPrompt = if (problemType == "aritmética" || true) {
                    systemPromptSocraticArithmetic(history.joinToString("\n\n---\n\n"))
                } else {
                    systemPromptSocratic(history.joinToString("\n\n---\n\n"))
                }

                Triple(socraticPrompt, WhiteboardFlowState.SOCRATIC_TUTORING, currentState.initialProblemStatement)
            }
            else -> {
                Log.w(TAG, "onSendText called in unexpected state: ${currentState.flowState}")
                return
            }
        }

        val isInterpreting = newFlowState == WhiteboardFlowState.INTERPRETING

        _uiState.update { it.copy(
            textInput = "",
            flowState = newFlowState,
            initialProblemStatement = newProblemStatement,
            tutorMessage = if (isInterpreting) "Estoy leyendo el problema..." else null,
            hint = null,
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
            val currentGrid = it.gridItems.toMutableMap()
            // Add the confirmed problem statement to the grid
            getNextGridPosition(it)?.let { pos ->
                currentGrid[pos] = WhiteboardItem.Expression(problemStatementFromTutor)
            }

            it.copy(
                flowState = WhiteboardFlowState.SOCRATIC_TUTORING,
                tutorMessage = null, // Clear interpretation message
                hint = null,
                initialProblemStatement = problemStatementFromTutor,
                gridItems = currentGrid
            ).also {
                Log.d(TAG, "State updated for confirmation accept. New flow state: ${it.flowState}")
            }
        }

        // Kick off the Socratic dialogue
        viewModelScope.launch {
            val socraticPrompt = systemPromptSocraticArithmetic("Tutor found problem statement: $problemStatementFromTutor") + "\n\nNow, begin the conversation with a guiding question."
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
                flowState = WhiteboardFlowState.INITIAL,
                gridItems = emptyMap(),
                tutorMessage = null,
                hint = null,
                initialProblemStatement = "",
                showConfirmationFailureMessage = true
            ).also {
                Log.d(TAG, "State reset after rejection.")
            }
        }
    }
}
