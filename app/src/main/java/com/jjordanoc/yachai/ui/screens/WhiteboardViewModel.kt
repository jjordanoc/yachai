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


@Serializable
data class SideLengths(
    @SerialName("AC") val ac: String,
    @SerialName("AB") val ab: String,
    @SerialName("BC") val bc: String
)

@Serializable
data class AnimationArgs(
    val sideLengths: SideLengths? = null,
    val point: String? = null,
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
    @SerialName("tutor_message") val tutorMessage: String,
    val hint: String,
    val animation: List<AnimationCommand>
)

sealed class WhiteboardItem {
    data class DrawingPath(val path: Path, val color: Color, val strokeWidth: Float) : WhiteboardItem()
    data class AnimatedTriangle(
        val a: Offset,
        val b: Offset,
        val c: Offset,
        val sideLengths: SideLengths,
        val tutorMessage: String,
        val highlightedSides: List<String> = emptyList(),
        val highlightedAngle: String? = null
    ) : WhiteboardItem()
}

data class WhiteboardState(
    val items: List<WhiteboardItem> = emptyList(),
    val textInput: String = ""
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

val systemPromptSocratic = """
Eres un tutor de matemáticas experto, amigable y paciente. Estás ayudando a un estudiante a resolver un problema de geometría mediante una conversación paso a paso. Utilizas texto en español y animaciones sobre una pizarra digital.

Tienes acceso a un historial de los últimos dos turnos de conversación, incluyendo lo que el estudiante respondió y lo que tú mostraste anteriormente.

Tu objetivo es guiar al estudiante para que encuentre la solución por sí mismo. No debes dar la respuesta directamente, ni escribir el resultado final. En su lugar, haz preguntas que ayuden al estudiante a razonar.

La figura central es siempre un triángulo con vértices A, B y C. El ángulo en el punto B es recto.

Puedes usar los siguientes comandos de animación. Solo puedes usar estos comandos y sus argumentos exactamente como se describen:

1. drawRightTriangle  
  args:  
    sideLengths: un objeto con claves "AB", "BC", "AC" y valores numéricos o "x"

2. highlightSide  
  args:  
    segment: nombre del lado ("AB", "BC", "AC")  
    label (opcional): una etiqueta como "x"

3. highlightAngle  
  args:  
    point: vértice de la figura ("A", "B", "C")  
    type (opcional): solo "right"

Tu respuesta debe estar en el siguiente formato JSON (exactamente así):

{
  "tutor_message": "TEXTO EN ESPAÑOL",
  "hint": "TEXTO EN ESPAÑOL",
  "animation": [
    { "command": "COMANDO", "args": { ... } },
    ...
  ]
}

Instrucciones finales:
- No incluyas explicaciones fuera del JSON.
- No escribas ningún comentario ni justificación.
- Solo responde con un único objeto JSON válido.
- Toda la comunicación visible debe estar en español.
- Tu estilo debe ser motivador y basado en preguntas guiadas.
""".trimIndent()

class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WhiteboardState())
    val uiState: StateFlow<WhiteboardState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val viewModelScope = CoroutineScope(Dispatchers.IO)


    init {
        LlmHelper.switchDataSource(LlmHelper.DataSourceType.AZURE, application)
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
                                val sideAB = sideLengths.ab.toFloatOrNull() ?: 5f
                                val sideAC = sideLengths.ac.toFloatOrNull() ?: 13f
                                val sideBC = sqrt(sideAC * sideAC - sideAB * sideAB)

                                val drawScale = 30f

                                val pointA = Offset(0f, -sideAB * drawScale)
                                val pointB = Offset.Zero
                                val pointC = Offset(sideBC * drawScale, 0f)

                                currentTriangle = WhiteboardItem.AnimatedTriangle(
                                    a = pointA,
                                    b = pointB,
                                    c = pointC,
                                    sideLengths = sideLengths,
                                    tutorMessage = response.tutorMessage
                                )
                            }
                        }
                        "highlightSide" -> {
                            command.args.segment?.let { segment ->
                                currentTriangle = currentTriangle?.copy(
                                    highlightedSides = (currentTriangle?.highlightedSides ?: emptyList()) + segment
                                )
                            }
                        }
                        "highlightAngle" -> {
                            command.args.point?.let { point ->
                                currentTriangle = currentTriangle?.copy(highlightedAngle = point)
                            }
                        }
                        else -> {
                            Log.w(TAG, "Unknown animation command: ${command.command}")
                        }
                    }
                }

                currentTriangle = currentTriangle?.copy(tutorMessage = response.tutorMessage)

                if (currentTriangle != null) {
                    val otherItems = currentState.items.filterNot { it is WhiteboardItem.AnimatedTriangle }
                    currentState.copy(items = otherItems + currentTriangle!!)
                } else {
                    currentState
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LLM response", e)
        }
    }


    fun onTextInputChanged(newText: String) {
        _uiState.update { it.copy(textInput = newText) }
    }

    fun onSendText() {
        val currentText = _uiState.value.textInput
        if (currentText.isBlank()) return

        _uiState.update { it.copy(textInput = "") }

        viewModelScope.launch {
            val placeholderPrompt = "" // Using a placeholder as requested
            var fullResponse = ""

            LlmHelper.runInference(
                input = systemPromptInterpret,
                resultListener = { partialResult, done ->
                    fullResponse += partialResult
                    if (done) {
                        CoroutineScope(Dispatchers.Main).launch {
                            processLlmResponse(fullResponse)
                        }
                    }
                }
            )
        }
    }

    fun addPath(path: Path, color: Color, strokeWidth: Float) {
        val newPath = WhiteboardItem.DrawingPath(path, color, strokeWidth)
        _uiState.update { it.copy(items = it.items + newPath) }
    }
}
