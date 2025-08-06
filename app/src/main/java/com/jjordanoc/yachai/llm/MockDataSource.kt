package com.jjordanoc.yachai.llm

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MockDataSource : LlmDataSource {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Hardcoded response for testing
    private val mockResponse = """
```json
[
  {
    "tutor_message": "¡Hola! Vamos a ayudar a la familia Quispe a construir su corral. Primero, dibujaremos el corral con las dimensiones que nos da.",
    "animations": [
      { "command": "drawRectangle", "args": { "length": "8", "width": "5", "showGrid": "true" } }
    ]
  },
  {
    "tutor_message": "Ahora, para saber cuántos metros cuadrados necesitan, vamos a calcular el área del corral. El área de un rectángulo es largo por ancho, ¿verdad?",
    "animations": [
          { "command": "drawRectangle", "args": { "length": "8", "width": "5", "showGrid": "true" } },

       { "command": "drawExpression", "args": { "expression": "Largo × Ancho = Área" } }
    ]
  },
  {
    "tutor_message": "Así que, vamos a multiplicar el largo (8 metros) por el ancho (5 metros). ¡Mmm, qué rápido! Podemos pensar en ello como contar cuadritos.",
    "animations": [
          { "command": "drawRectangle", "args": { "length": "8", "width": "5", "showGrid": "true" } },
       { "command": "drawExpression", "args": { "expression": "8 × 5 = ?" } }
    ]
  },
  {
    "tutor_message": "¡Ah! ¡Vamos a calcularlo! 8 veces 5 son 40. ¡Perfecto!",
    "animations": [
          { "command": "drawRectangle", "args": { "length": "8", "width": "5", "showGrid": "true" } },
      { "command": "drawExpression", "args": { "expression": "8 × 5 = 40" } }
    ]
  },
  {
    "tutor_message": "¡Listo! El área del corral es de 40 metros cuadrados. Ahora la familia Quispe sabrá cuántos metros cuadrados de terreno necesitan.",
    "animations": [
       { "command": "drawExpression", "args": { "expression": "Área = 40 m²" } }
    ]
  }
]
```
    """.trimIndent()

    override suspend fun initialize() {
        // Simulate some initialization delay
        delay(100)
    }

    override fun runInference(input: String, images: List<Bitmap>, resultListener: ResultListener) {
        scope.launch {
            try {
                if (input.contains("llave", ignoreCase = true)) {
                    resultListener("recibido", true)
                } else {
                    // Send final result
                    resultListener(mockResponse, true)
                }
            } catch (e: Exception) {
                resultListener("Mock Error: ${e.message}", true)
            }
        }
    }

    override fun sizeInTokens(text: String): Int {
        // Return a mock token count (roughly 1 token per 4 characters)
        return text.length / 4
    }

    override fun cleanUp() {
        // No cleanup needed for mock data source
    }
}
