package com.jjordanoc.yachai.ui.screens

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.jjordanoc.yachai.utils.TAG
import kotlin.math.sqrt


@Serializable
data class TutorMessage(
    val text: String,
    val anchor: String,
    val position: String
)

@Serializable
data class SideLengths(
    @SerialName("AC") val ac: String,
    @SerialName("AB") val ab: String,
    @SerialName("BC") val bc: String
)

@Serializable
data class AnimationArgs(
    val sideLengths: SideLengths
)

@Serializable
data class AnimationCommand(
    val command: String,
    val args: AnimationArgs
)

@Serializable
data class LlmResponse(
    @SerialName("tutor_message") val tutorMessage: TutorMessage,
    val hint: String,
    val animation: List<AnimationCommand>
)

sealed class WhiteboardItem {
    data class DrawingPath(val path: Path, val color: Color, val strokeWidth: Float) : WhiteboardItem()
    data class AnimatedTriangle(val a: Offset, val b: Offset, val c: Offset, val sideLengths: SideLengths, val tutorMessage: TutorMessage) : WhiteboardItem()
}

data class WhiteboardState(
    val items: List<WhiteboardItem> = emptyList(),
    val textInput: String = ""
)

class WhiteboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WhiteboardState())
    val uiState: StateFlow<WhiteboardState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun processLlmResponse(jsonString: String) {
        Log.d(TAG, "Processing LLM response: $jsonString")
        try {
            val response = json.decodeFromString<LlmResponse>(jsonString)
            Log.d(TAG, "LLM response parsed successfully: $response")
            val command = response.animation.firstOrNull { it.command == "drawRightTriangle" }

            if (command != null) {
                Log.d(TAG, "drawRightTriangle command found with args: ${command.args}")
                // Use fixed values for drawing proportions, but labels will be from the JSON
                val sideAB = command.args.sideLengths.ab.toFloatOrNull() ?: 5f
                val sideAC = command.args.sideLengths.ac.toFloatOrNull() ?: 13f
                val sideBC = sqrt(sideAC * sideAC - sideAB * sideAB)

                val drawScale = 30f

                // Define points relative to B at origin
                val pointA = Offset(0f, -sideAB * drawScale)
                val pointB = Offset.Zero
                val pointC = Offset(sideBC * drawScale, 0f)

                val triangleItem = WhiteboardItem.AnimatedTriangle(
                    a = pointA,
                    b = pointB,
                    c = pointC,
                    sideLengths = command.args.sideLengths,
                    tutorMessage = response.tutorMessage
                )
                Log.d(TAG, "Creating AnimatedTriangle item: $triangleItem")
                _uiState.update { it.copy(items = it.items + triangleItem) }
            } else {
                Log.w(TAG, "No 'drawRightTriangle' command found in LLM response.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LLM response", e)
        }
    }


    fun onTextInputChanged(newText: String) {
        _uiState.update { it.copy(textInput = newText) }
    }

    fun onSendText() {
        // This will eventually add the text to the whiteboard as a message from the user
        // and probably trigger a response from the AI.
        // For now, let's just clear the input.
        _uiState.update { it.copy(textInput = "") }
    }

    fun addPath(path: Path, color: Color, strokeWidth: Float) {
        val newPath = WhiteboardItem.DrawingPath(path, color, strokeWidth)
        _uiState.update { it.copy(items = it.items + newPath) }
    }
}
