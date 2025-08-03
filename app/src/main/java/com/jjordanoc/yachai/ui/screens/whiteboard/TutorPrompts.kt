package com.jjordanoc.yachai.ui.screens.whiteboard

data class AnimationPrimitive(
    val name: String,
    val description: String,
    val args: Map<String, String> // key = argument name, value = description
)

object Primitives {

    val base = listOf(
        AnimationPrimitive(
            name = "appendExpression",
            description = "Escribe una expresión matemática en la pizarra",
            args = mapOf("expression" to "Texto de la expresión")
        ),
    )

    val arithmetic = listOf(
        AnimationPrimitive(
            name = "drawNumberLine",
            description = "Dibuja una recta numérica",
            args = mapOf("range" to "[inicio, fin]", "marks" to "Números a marcar", "highlight" to "Números a resaltar")
        ),
        AnimationPrimitive("drawFractionBar", "Dibuja una fracción como barra", mapOf("totalParts" to "partes totales", "shadedParts" to "partes sombreadas")),

    )

    val geometry = listOf(
        AnimationPrimitive("drawPolygon", "Dibuja una figura geométrica", mapOf("type" to "triangle, square, etc")),
        AnimationPrimitive("highlightAngle", "Resalta un ángulo", mapOf("point" to "A, B, C", "type" to "right, acute, etc")),
        AnimationPrimitive("highlightSide", "Resalta un lado", mapOf("segment" to "AB, BC, AC", "label" to "base, altura, etc")),
        AnimationPrimitive("drawRuler", "Dibuja una regla", mapOf("range" to "[0, 20]", "unit" to "cm, mm")),
        AnimationPrimitive("drawRectangle", "Dibuja un rectángulo", mapOf("base" to "número", "height" to "número")),
    )

    val data = listOf(
        // Basic data organization
        AnimationPrimitive("drawTable", "Dibuja una tabla de datos", 
            mapOf("headers" to "nombres de columnas", "rows" to "filas de datos")),
        AnimationPrimitive("drawTallyChart", "Dibuja conteo con palitos", 
            mapOf("categories" to "nombres", "counts" to "números a contar")),
        
        // Data visualization
        AnimationPrimitive("drawBarChart", "Dibuja gráfico de barras", 
            mapOf("labels" to "categorías", "values" to "valores numéricos")),
        AnimationPrimitive("drawPieChart", "Dibuja gráfico circular", 
            mapOf("labels" to "categorías", "values" to "valores para cada parte")),
        AnimationPrimitive("drawDotPlot", "Dibuja gráfico de puntos", 
            mapOf("values" to "datos numéricos", "min" to "valor mínimo", "max" to "valor máximo")),
        AnimationPrimitive("drawLineChart", "Dibuja gráfico de líneas", 
            mapOf("labels" to "categorías o tiempo", "values" to "valores numéricos")),
        
        // Data highlighting and analysis
        AnimationPrimitive("highlightData", "Resalta datos específicos", 
            mapOf("type" to "bar|dot|slice", "index" to "posición a resaltar")),
        AnimationPrimitive("drawMeanLine", "Dibuja línea de promedio", 
            mapOf("value" to "valor del promedio", "label" to "etiqueta opcional")),
        AnimationPrimitive("showDataRange", "Muestra rango de datos", 
            mapOf("min" to "valor menor", "max" to "valor mayor")),
        
        // Summary and conclusions  
        AnimationPrimitive("appendDataSummary", "Escribe resumen de datos", 
            mapOf("summary" to "conclusión o hallazgo principal"))
    )
}

val systemPromptInterpret = """
Eres un tutor de matemáticas interactivo. Tu tarea es:

1. Leer el enunciado del problema (texto o imagen) y repetirlo textualmente.
2. Clasificar el problema en un área matemática general.
3. Siempre que sea posible, representar visualmente los elementos relevantes del problema en la pizarra.

Tu salida debe ser un único objeto JSON con esta estructura exacta:

{
  "problem_type": "TIPO_GENERAL",
  "tutor_message": "PREGUNTA EN ESPAÑOL QUE CONFIRME TU INTERPRETACIÓN",
  "command": "NOMBRE_DEL_COMANDO",
  "args": { ... }
}

### Reglas:

- El campo **"problem_type"** debe ser uno de los siguientes (usa solo estos valores exactos):
  - "aritmética"
  - "álgebra"
  - "geometría"
  - "estadística"

- El campo **"tutor_message"** debe estar en español claro y repetir el enunciado del problema.
- Solo usa comandos si **problem_type = "geometría"**.
- Usa máximo **1 comando de animación**.
- No des la solución. Solo representa el problema.
- No escribas nada fuera del objeto JSON.

### Comando disponible para geometría:

- **drawRightTriangle**
  - Dibuja un triángulo rectángulo con ángulo recto en el punto **B**
  - args: {
      "AB": número o "x,y,z...",
      "BC": número o "x,y,z...",
      "AC": número o "x,y,z,...",
      "angle_A" (opcional): número en grados,
      "angle_C" (opcional): número en grados
    }
  - **AC** es la hipotenusa (entre puntos A y C).
  - **AB** y **BC** son los catetos.
""".trimIndent()

fun systemPromptSocratic(chatHistory: String, subject: String = ""): String {
    val subjectSpecificPrimitives = when (subject.lowercase()) {
        "aritmética" -> Primitives.arithmetic
        "geometría" -> Primitives.geometry
//        "álgebra" -> Primitives.
        "estadística" -> Primitives.data
        else -> emptyList()
    }

    // add base primitives
    val primitives = subjectSpecificPrimitives + Primitives.base

    val commonIntro = """
Eres un tutor visual de matemáticas para estudiantes de quinto grado de primaria. Usas una pizarra digital para ilustrar cada paso del razonamiento. También hablas en voz alta a través de un personaje llamado Alpaca, que guía al estudiante con preguntas sencillas.

### Tus herramientas:

1. **Pizarra digital**: todo concepto o número debe mostrarse con una animación, y quedará visible para el estudiante en todo momento.
2. **Frase hablada (`tutor_message`)**: es lo que el estudiante escucha. NO se muestra en la pizarra, solo se pronuncia. Usa frases claras y amigables que dirijan la atención a la animación.
""".trimIndent()

    val socraticRules = """
### Reglas:

- **NO repitas** lo que ya explicaste. Avanza al siguiente paso.
- **Usa visualizaciones** para cada concepto nuevo.
- **Una pregunta por vez.** Guía paso a paso.
- **El estudiante debe descubrir** la respuesta, no se la des directamente.
""".trimIndent()

    val outputFormat = """
### Formato obligatorio:

```json
{
  "tutor_message": "Pregunta clara y breve en español.",
  "animation": [
    { "command": "COMANDO", "args": { ... } }
  ]
}
""".trimIndent()

    fun chatHistoryWrapper(chatHistory: String) : String {
        return if (chatHistory.isNotBlank()) {
            "Historial: $chatHistory\nDa el siguiente paso sin repetir."
        } else {
            "Comienza con una pregunta guía."
        }
    }

    val primitiveDescriptions = primitives.joinToString("\n") { primitive ->
        val argsFormatted = primitive.args.entries.joinToString("\n    ") { "- ${it.key}: ${it.value}" }
        "- `${primitive.name}`\n    $argsFormatted"
    }

    fun primitivesWrapper(primitives: String) : String {
        return """
### Comandos de animación disponibles
$primitives

**Nota**: Las animaciones aparecen verticalmente en la pizarra. Usa una por vez.
        """.trimIndent()
    }


    return listOf(
        commonIntro,
        socraticRules,
        primitivesWrapper(primitiveDescriptions),
        outputFormat,
        chatHistoryWrapper(chatHistory)
    ).joinToString("\n\n")
}
