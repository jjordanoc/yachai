package com.jjordanoc.yachai.llm.data

data class AnimationPrimitive(
    val name: String,
    val description: String,
    val args: Map<String, String> // key = argument name, value = description
)

object Primitives {

    val base = listOf(
        AnimationPrimitive(
            name = "drawExpression",
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
    )

    val geometry = listOf(
        AnimationPrimitive("drawRectangle", "Dibuja un rectángulo", mapOf("length" to "número (largo)", "width" to "número (ancho)")),
        AnimationPrimitive("drawGrid", "Dibuja una cuadrícula sobre el rectángulo para mostrar área", mapOf("length" to "número (largo)", "width" to "número (ancho)", "unit" to "unidad de medida (ej: 1m², 1cm²)")),
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


fun systemPrompt(): String {

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
- **"animations"**: Lista completa de animaciones visibles en este paso

Mantén cada paso simple y enfocado en UNA SOLA IDEA VISUAL NUEVA POR PASO.
"""

    val outputFormat = """
### Ejemplo: 
Problema: "María quiere cercar un jardín rectangular de 6 metros de largo y 4 metros de ancho. ¿Cuántos metros cuadrados tiene el jardín?"

### Formato obligatorio de respuesta:
```json
[
  {
    "tutor_message": "Voy a dibujar el corral de la familia Quispe.",
    "animations": [
      { "command": "drawRectangle", "args": { "length": "8", "width": "5" } }
    ]
  },
  {
    "tutor_message": "Ahora voy a dividirlo en cuadritos para poder CONTAR el área.",
    "animations": [
      { "command": "drawRectangle", "args": { "length": "8", "width": "5" } },
      { "command": "drawGrid", "args": { "length": "8", "width": "5", "unit": "1m²" } }
    ]
  },
  {
    "tutor_message": "¡Perfecto! Puedo contar 8 cuadritos por fila y 5 filas. Esto me da la fórmula.",
    "animations": [
      { "command": "drawRectangle", "args": { "length": "8", "width": "5" } },
      { "command": "drawGrid", "args": { "length": "8", "width": "5", "unit": "1m²" } },
      { "command": "drawExpression", "args": { "expression": "8 × 5 = 40 m²" } }
    ]
  }
]
""".trimIndent()


    fun primitivesWrapper(primitives: List<AnimationPrimitive>) : String {
        val primitiveDescriptions = primitives.joinToString("\n") { primitive ->
            val argsFormatted = primitive.args.entries.joinToString("\n    ") { "- ${it.key}: ${it.value}" }
            "- `${primitive.name}`: ${primitive.description}\n    $argsFormatted"
        }
        return """
        ### Comandos de animación disponibles
        $primitiveDescriptions
        """.trimIndent()
    }

    return listOf(
        commonIntro,
        thinkAloudRules,
        multiStepFormat,
        primitivesWrapper(Primitives.base + Primitives.geometry),
        outputFormat,
    ).joinToString("\n\n")
}
