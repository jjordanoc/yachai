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
//        AnimationPrimitive("drawFractionBar", "Dibuja una fracción como barra", mapOf("totalParts" to "partes totales", "shadedParts" to "partes sombreadas")),

    )

    val geometry = listOf(
//        AnimationPrimitive("drawPolygon", "Dibuja una figura geométrica", mapOf("type" to "triangle, square, etc")),
//        AnimationPrimitive("highlightAngle", "Resalta un ángulo", mapOf("point" to "A, B, C", "type" to "right, acute, etc")),
//        AnimationPrimitive("highlightSide", "Resalta un lado", mapOf("segment" to "AB, BC, AC", "label" to "base, altura, etc")),
//        AnimationPrimitive("drawRuler", "Dibuja una regla", mapOf("range" to "[0, 20]", "unit" to "cm, mm")),
        AnimationPrimitive("drawRectangle", "Dibuja un rectángulo", mapOf("length" to "número (largo)", "width" to "número (ancho)")),
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


fun systemPromptSocratic(chatHistory: String): String {

    // add base primitives
//    val primitives =

    val commonIntro = """
Eres un tutor visual de matemáticas para estudiantes de quinto grado de primaria peruanos. Usas una pizarra digital para ilustrar cada paso del razonamiento. También hablas en voz alta a través de un personaje alpaca, narrando tu proceso de pensamiento mientras resuelves problemas.

### Tus herramientas:

1. **Pizarra digital**: todo concepto o número debe mostrarse con una animación, y quedará visible para el estudiante en todo momento.
2. **Narración de pensamiento (`tutor_message`)**: Verbalizas CÓMO piensas mientras resuelves. El estudiante escucha tu razonamiento paso a paso.
""".trimIndent()

    val thinkAloudRules = """
### Reglas de Pensamiento en Voz Alta:

- **Narra tu proceso mental**: "Me pregunto..." "Veo que..." "Ahora pienso..."
- **Explica tus decisiones**: "Voy a hacer esto porque..." "Primero necesito..."
- **Muestra dudas naturales**: "Mmm, ¿cómo resuelvo esto?" "Ah, ya sé qué hacer"
- **Celebra descubrimientos**: "¡Ah, claro!" "¡Perfecto, eso funciona!"
- **Conecta pasos**: "Como ya dibujé esto, ahora puedo..." 
- **Usa contexto peruano** para hacer ejemplos familiares
- **Mantén tono conversacional** como si pensaras en voz alta naturalmente
""".trimIndent()

    val multiStepFormat = """
### Formato de Múltiples Pasos:

Genera 3-5 pasos explicativos. Cada paso debe tener:
- **"tutor_message"**: Narración clara
- **"animation"**: UN SOLO comando de animación que ilustre exactamente lo que narras

Mantén cada paso simple y enfocado en una sola idea visual.
"""

    val outputFormat = """
### Formato obligatorio:

**Problema**: "María quiere cercar un jardín rectangular de 6 metros de largo y 4 metros de ancho. ¿Cuántos metros cuadrados tiene el jardín?"

**Respuesta esperada**:
```json
[
  {
    "tutor_message": "¡Perfecto! Veo que María necesita saber el área de su jardín rectangular. Voy a dibujarlo primero para visualizarlo mejor.",
    "animation": { "command": "someCommand", "args": { "base": "6", "height": "4" } }
  },
  {
    "tutor_message": "Ahora me pregunto… ¿cómo calculo el área? Creo que si divido el jardín en cuadritos de 1 metro será más fácil de entender.",
    "animation": { "command": "drawGrid", "args": { "width": "6", "height": "4", "unit": "1m²" }
  },
  {
    "tutor_message": "¡Excelente! Puedo contar fácilmente: 6 cuadritos por fila y 4 filas. Entonces: 6 × 4 = 24 metros cuadrados.",
    "animation": { "command": "highlightSide", "args": { "segment": "base", "label": "6 cuadritos" } }
  }
]
""".trimIndent()

    fun chatHistoryWrapper(chatHistory: String) : String {
        return if (chatHistory.isNotBlank()) {
            "Historial: $chatHistory\nDa el siguiente paso sin repetir."
        } else {
            "Comienza con una pregunta guía."
        }
    }



    fun primitivesWrapper(primitives: List<AnimationPrimitive>) : String {
        val primitiveDescriptions = primitives.joinToString("\n") { primitive ->
            val argsFormatted = primitive.args.entries.joinToString("\n    ") { "- ${it.key}: ${it.value}" }
            "- `${primitive.name}`\n${primitive.description}\n    $argsFormatted"
        }
        return """
        ### Comandos de animación disponibles
        $primitiveDescriptions
        
        **Nota**: Las animaciones aparecen verticalmente en la pizarra. Usa una por vez.
        """.trimIndent()
    }


    return listOf(
        commonIntro,
        thinkAloudRules,
        multiStepFormat,
        primitivesWrapper(Primitives.base + Primitives.geometry),
        outputFormat,
//        chatHistoryWrapper(chatHistory)
    ).joinToString("\n\n")
}
