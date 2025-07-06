package com.jjordanoc.yachai.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import com.jjordanoc.yachai.data.Models
import com.jjordanoc.yachai.data.getLocalPath
import com.jjordanoc.yachai.utils.SettingsManager
import com.jjordanoc.yachai.data.ModelConfig


enum class WhiteboardFlowState {
    INITIAL,
    INTERPRETING,
    AWAITING_CONFIRMATION,
    SOCRATIC_TUTORING
}

object LenientStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonInput = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonInput.decodeJsonElement()

        if (element is JsonNull) {
            return null
        }

        if (element is JsonPrimitive) {
            return element.content
        }

        return element.toString()
    }
}

@Serializable
data class SideLengths(
    @SerialName("AC") @Serializable(with = LenientStringSerializer::class) val ac: String?,
    @SerialName("AB") @Serializable(with = LenientStringSerializer::class) val ab: String?,
    @SerialName("BC") @Serializable(with = LenientStringSerializer::class) val bc: String?
)

@Serializable
data class AnimationArgs(
    val sideLengths: SideLengths? = null,
    val point: String?  = null,
    val type: String? = null,
    val segment: String? = null,
    val label: String? = null
)

@Serializable
data class AnimationCommand(
    val command: String,
    val args: AnimationArgs
)

@Serializable
data class LlmResponse(
    @SerialName("tutor_message") val tutorMessage: String?,
    val hint: String?,
    val animation: List<AnimationCommand> = emptyList()
)

sealed class WhiteboardItem {
    data class DrawingPath(val path: Path, val color: Color, val strokeWidth: Float) : WhiteboardItem()
    data class AnimatedTriangle(
        val a: Offset,
        val b: Offset,
        val c: Offset,
        val sideLengths: SideLengths,
        val highlightedSides: List<String> = emptyList(),
        val highlightedAngle: String? = null
    ) : WhiteboardItem()
}

data class WhiteboardState(
    val items: List<WhiteboardItem> = emptyList(),
    val textInput: String = "",
    val flowState: WhiteboardFlowState = WhiteboardFlowState.INITIAL,
    val initialProblemStatement: String = "",
    val tutorMessage: String? = null,
    val hint: String? = null,
    val showConfirmationFailureMessage: Boolean = false,
    val selectedImageUri: Uri? = null
)

val systemPromptInterpret = """
Eres un tutor de matemáticas interactivo. Tu tarea es:

1. Leer el enunciado de un problema (puede venir de texto o imagen).
2. Clasificar el problema según su área matemática general.
3. Interpretar el problema y verificar con el estudiante si tu interpretación es correcta.
4. Representar visualmente los elementos relevantes en la pizarra.

Tu salida debe ser un único objeto JSON con esta estructura:

{
  "problem_type": "TIPO_GENERAL",
  "tutor_message": "PREGUNTA EN ESPAÑOL QUE CONFIRME TU INTERPRETACIÓN",
  "hint": "AYUDA OPCIONAL EN ESPAÑOL",
  "animation": [ { "command": ..., "args": { ... } } ]
}

### Reglas:

- El campo "problem_type" debe ser uno de los siguientes (usa solo estos valores exactos):
  - "aritmética"
  - "álgebra"
  - "geometría"
  - "medición"
  - "estadística"
  - "probabilidad"
  - "números y operaciones"
  - "funciones"
  - "razonamiento lógico"
  - "otros" (si no encaja claramente en otra categoría)

- "tutor_message" debe estar en español claro y debe confirmar si entendiste correctamente el problema.
- Usa una animación relevante si el problema es visual (por ejemplo, en geometría).
- No des la solución, solo representa el problema.
- No expliques fuera del JSON.
- Solo puedes usar comandos de animación si el tipo de problema es "geometría" o "medición", y debes limitarte a los siguientes:

  1. drawRightTriangle
     - args: { sideLengths: { "AB": ..., "BC": ..., "AC": ... } }

  2. highlightSide
     - args: { segment: "AB" | "BC" | "AC", label (opcional) }

  3. highlightAngle
     - args: { point: "A" | "B" | "C", type (opcional): "right" }

No escribas nada fuera del objeto JSON.
""".trimIndent()

fun systemPromptSocratic(chatHistory: String): String {
    return """
Eres un tutor de matemáticas experto, amigable y paciente. Estás ayudando a un estudiante a resolver un problema de geometría mediante una conversación paso a paso. Utilizas texto en español y animaciones sobre una pizarra digital.

Siempre debes usar el estilo socrático: no debes dar la respuesta directamente. Haz preguntas que ayuden al estudiante a razonar por sí mismo.

### Contexto:
Tienes acceso al historial de los últimos dos turnos de conversación. Cada turno contiene lo que el estudiante dijo y lo que tú mostraste anteriormente (mensaje, pista y animaciones).

La figura central en la pizarra es un triángulo con vértices A, B y C. El ángulo en el punto B es recto.

### Historial reciente:
$chatHistory

### Tu tarea:
Basado en el historial y la última respuesta del estudiante, continúa la conversación con un nuevo paso. Tu objetivo es avanzar el razonamiento del estudiante, despejar dudas y fortalecer su comprensión. No reveles resultados finales.

### Comandos de animación permitidos:
Usa solo los siguientes comandos exactamente como están descritos. Cada paso debe contener entre **1 y 3 animaciones** cuidadosamente seleccionadas para **maximizar el valor educativo y aclarar posibles malentendidos**.

1. **highlightSide**  
  args:  
    segment: "AB", "BC" o "AC"  
    label (opcional): texto corto como "hipotenusa", "x", etc.

2. **highlightAngle**  
  args:  
    point: "A", "B" o "C"

3. **appendExpression**  
  args:  
    expression: expresión nueva que se añade a la pizarra como "4² = 16"

### Formato de salida (debes seguirlo exactamente):
Responde con un único objeto JSON en el siguiente formato:

{
  "tutor_message": "TEXTO EN ESPAÑOL",
  "hint": "TEXTO EN ESPAÑOL",
  "animation": [
    { "command": "COMANDO", "args": { ... } }
  ]
}

### Instrucciones finales:
- No incluyas explicaciones fuera del JSON.
- No escribas ningún comentario ni justificación.
- Toda la comunicación visible debe estar en español.
- Mantén un tono amigable, motivador y guiado por preguntas.
- Usa como máximo 3 animaciones por paso.
""".trimIndent()
}


class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WhiteboardState())
    val uiState: StateFlow<WhiteboardState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val viewModelScope = CoroutineScope(Dispatchers.IO)


    init {
        Log.d(TAG, "WhiteboardViewModel initialized.")
        val context = getApplication<Application>().applicationContext
        val settingsManager = SettingsManager(context)
        val model = Models.GEMMA_3N_E2B_VISION

        val modelConfig = ModelConfig(
            modelPath = model.getLocalPath(context),
            useGpu = settingsManager.isGpuEnabled()
        )

        LlmHelper.switchDataSource(
            type = LlmHelper.DataSourceType.MEDIAPIPE,
            context = context,
            modelConfig = modelConfig
        )
    }

    fun processLlmResponse(jsonString: String) {
        Log.d(TAG, "Processing LLM response: $jsonString")
        try {
            val response = json.decodeFromString<LlmResponse>(jsonString)
            Log.d(TAG, "LLM response parsed successfully: $response")

            _uiState.update { currentState ->
                var currentTriangle = currentState.items.filterIsInstance<WhiteboardItem.AnimatedTriangle>().firstOrNull()

                for (command in response.animation) {
                    when (command.command) {
                        "drawRightTriangle" -> {
                            command.args.sideLengths?.let { sideLengths ->
                                Log.d(TAG, "drawRightTriangle command found with args: ${command.args}")
                                val sideAB = sideLengths.ab?.toFloatOrNull() ?: 5f
                                val sideAC = sideLengths.ac?.toFloatOrNull() ?: 13f
                                val sideBC = sqrt(sideAC * sideAC - sideAB * sideAB)

                                val drawScale = 30f

                                val pointA = Offset(0f, -sideAB * drawScale)
                                val pointB = Offset.Zero
                                val pointC = Offset(sideBC * drawScale, 0f)

                                currentTriangle = WhiteboardItem.AnimatedTriangle(
                                    a = pointA,
                                    b = pointB,
                                    c = pointC,
                                    sideLengths = sideLengths
                                )
                            }
                        }
                        "highlightSide" -> {
                            command.args.segment?.let { segment ->
                                Log.d(TAG, "highlightSide command found with segment: $segment")
                                currentTriangle = currentTriangle?.copy(
                                    highlightedSides = (currentTriangle?.highlightedSides ?: emptyList()) + segment
                                )
                            }
                        }
                        "highlightAngle" -> {
                            command.args.point?.let { point ->
                                Log.d(TAG, "highlightAngle command found with point: $point")
                                currentTriangle = currentTriangle?.copy(highlightedAngle = point)
                            }
                        }
                        else -> {
                            Log.w(TAG, "Unknown animation command: ${command.command}")
                        }
                    }
                }

                val newFlowState = if (currentState.flowState == WhiteboardFlowState.INTERPRETING) {
                    WhiteboardFlowState.AWAITING_CONFIRMATION
                } else {
                    currentState.flowState
                }

                Log.d(TAG, "Updating flow state to $newFlowState")

                if (currentTriangle != null) {
                    val otherItems = currentState.items.filterNot { it is WhiteboardItem.AnimatedTriangle }
                    currentState.copy(
                        items = otherItems + currentTriangle!!,
                        tutorMessage = response.tutorMessage,
                        hint = response.hint,
                        flowState = newFlowState
                    ).also {
                        Log.d(TAG, "State updated with new triangle and tutor message.")
                    }
                } else {
                    currentState.copy(
                        tutorMessage = response.tutorMessage,
                        hint = response.hint,
                        flowState = newFlowState
                    ).also {
                        Log.d(TAG, "State updated with new tutor message, no triangle changes.")
                    }
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
            WhiteboardFlowState.SOCRATIC_TUTORING -> {
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

                Triple(systemPromptSocratic(history.joinToString("\n\n---\n\n")), WhiteboardFlowState.SOCRATIC_TUTORING, currentState.initialProblemStatement)
            }
            else -> {
                Log.w(TAG, "onSendText called in unexpected state: ${currentState.flowState}")
                return
            }
        }

        _uiState.update { it.copy(
            textInput = "",
            flowState = newFlowState,
            initialProblemStatement = newProblemStatement,
            showConfirmationFailureMessage = false,
            selectedImageUri = null // Clear image after sending
        ).also {
            Log.d(TAG, "State updated for sending text. New flow state: ${it.flowState}")
        }}

        viewModelScope.launch {
            val fullPrompt = "$systemPrompt\n\nHere is the student's message:\n$currentText"
            Log.d(TAG, "Sending prompt to LLM (first 200 chars): ${fullPrompt.take(200)}")
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
                flowState = WhiteboardFlowState.SOCRATIC_TUTORING,
                tutorMessage = null, // Clear interpretation message
                hint = null,
                initialProblemStatement = problemStatementFromTutor
            ).also {
                Log.d(TAG, "State updated for confirmation accept. New flow state: ${it.flowState}")
            }
        }

        // Kick off the Socratic dialogue
        viewModelScope.launch {
            val socraticPrompt = systemPromptSocratic("Tutor found problem statement: $problemStatementFromTutor") + "\n\nNow, begin the conversation with a guiding question."
            Log.d(TAG, "Kicking off Socratic dialogue with prompt (first 200 chars): ${socraticPrompt.take(200)}")
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
                items = emptyList(),
                tutorMessage = null,
                hint = null,
                initialProblemStatement = "",
                showConfirmationFailureMessage = true
            ).also {
                Log.d(TAG, "State reset after rejection.")
            }
        }
    }

    fun addPath(path: Path, color: Color, strokeWidth: Float) {
        val newPath = WhiteboardItem.DrawingPath(path, color, strokeWidth)
        Log.d(TAG, "Adding new drawing path to state.")
        _uiState.update { it.copy(items = it.items + newPath) }
    }
}
