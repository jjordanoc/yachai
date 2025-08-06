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
"tutor_message": "Voy a dibujar el jardín. Necesitamos saber cómo calcular el costo de las semillas. Primero necesitamos calcular el área del jardín.",
"animations": [
  { "command": "drawRectangle", "args": { "length": "15", "width": "12", "drawAreaGrid": "true" } }
]
},
{
"tutor_message": "Ahora vamos a calcular el área del jardín.",
"animations": [
  { "command": "drawRectangle", "args": { "length": "15", "width": "12", "drawAreaGrid": "true" } },
  { "command": "drawExpression", "args": { "expression": "15 × 12 = 180 m²" } }
]
},
{
"tutor_message": "¡Excelente! Ahora que sabemos el área, podemos calcular cuánta semilla necesitamos.",
"animations": [
  { "command": "drawRectangle", "args": { "length": "15", "width": "12", "drawAreaGrid": "true" } },
  { "command": "drawExpression", "args": { "expression": "15 × 12 = 180 m²" } },
  { "command": "drawExpression", "args": { "expression": "180 m² × 50 g/m² = 9000 g" } }
]
},
{
"tutor_message": "Sabemos que necesitamos 9000 gramos de semillas. Podemos convertir eso en kilogramos dividiendo por 1000.",
"animations": [
  { "command": "drawRectangle", "args": { "length": "15", "width": "12", "drawAreaGrid": "true" } },
  { "command": "drawExpression", "args": { "expression": "15 × 12 = 180 m²" } },
  { "command": "drawExpression", "args": { "expression": "180 m² × 50 g/m² = 9000 g" } },
  { "command": "drawExpression", "args": { "expression": "9000 g / 1000 = 9 kg" } }
]
},
{
"tutor_message": "Ahora sabemos que necesitamos 9 kg de semillas. Necesitamos saber cuánta semilla cuesta por kilogramo.",
"animations": [
  { "command": "drawRectangle", "args": { "length": "15", "width": "12", "drawAreaGrid": "true" } },
  { "command": "drawExpression", "args": { "expression": "15 × 12 = 180 m²" } },
  { "command": "drawExpression", "args": { "expression": "180 m² × 50 g/m² = 9000 g" } },
  { "command": "drawExpression", "args": { "expression": "9000 g / 1000 = 9 kg" } },
  { "command": "drawExpression", "args": { "expression": "1 kg de semillas cuesta S/18.40" } }
]
},
{
"tutor_message": "Para averiguar cuánto debemos pagar por las semillas, vamos a multiplicar la cantidad de kilogramos que necesitamos por el precio por kilogramo.",
"animations": [
  { "command": "drawRectangle", "args":  { "length": "15", "width": "12", "drawAreaGrid": "true" } },
  { "command": "drawExpression", "args": { "expression": "15 × 12 = 180 m²" } },
  { "command": "drawExpression", "args": { "expression": "180 m² × 50 g/m² = 9000 g" } },
  { "command": "drawExpression", "args": { "expression": "9000 g / 1000 = 9 kg" } },
  { "command": "drawExpression", "args": { "expression": "1 kg de semillas cuesta S/18.40" } },
  { "command": "drawExpression", "args": { "expression": "9 kg × S/18.40/kg = S/165.60" } }
]
},
{
"tutor_message": "¡Listo! Así que, deberás pagar S/165.60 por las semillas que necesitas.",
"animations": [
  { "command": "drawRectangle", "args":  { "length": "15", "width": "12", "drawAreaGrid": "true" } },
  { "command": "drawExpression", "args": { "expression": "15 × 12 = 180 m²" } },
  { "command": "drawExpression", "args": { "expression": "180 m² × 50 g/m² = 9000 g" } },
  { "command": "drawExpression", "args": { "expression": "9000 g / 1000 = 9 kg" } },
  { "command": "drawExpression", "args": { "expression": "1 kg de semillas cuesta S/18.40" } },
  { "command": "drawExpression", "args": { "expression": "9 kg × S/18.40/kg = S/165.60" } },
  { "command": "drawExpression", "args": { "expression": "S/165.60" } }
]
}
]
```
    """.trimIndent()

    override suspend fun initialize() {
        // Simulate some initialization delay
        delay(5000)
    }

    override fun runInference(input: String, images: List<Bitmap>, resultListener: ResultListener) {
        scope.launch {

            try {
                if (input.contains("no entendi por que multiplicas", ignoreCase = true)) {
                    delay(3000) // Simulate processing delay
                    resultListener("Prueba contar los cuadraditos de cada fila y columna. Observa que contando los cuadraditos puedes obtener el área. Esto es multiplicación.", true)
                } else {
                    delay(5000) // Simulate processing delay
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
