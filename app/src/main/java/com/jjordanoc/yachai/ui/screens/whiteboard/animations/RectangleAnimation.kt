package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import java.util.UUID

/**
 * Animation for drawing a rectangle with dimensions and labels.
 * The rectangle is left-center aligned in the available whiteboard space.
 */
class RectangleAnimation(
    val length: Int,
    val width: Int,
    val lengthLabel: String = "longitud",
    val widthLabel: String = "ancho",
    override val id: String = UUID.randomUUID().toString()
) : MathAnimation {
    
    // Educational color hierarchy for whiteboard
    private val baseWhite = Color(0xFFFFFBF0) // Level 1: Base objects (main shapes)
    private val secondaryTeal = Color(0xFF4DB6AC) // Level 2: Secondary elements
    private val focusAmber = Color(0xFFFFC107) // Level 3: Focus & highlights (dimensions)
    private val criticalYellow = Color(0xFFFFE082) // Level 4: Critical info (highlights)
    
    override fun draw(drawScope: DrawScope) {
        val canvasSize = drawScope.size
        
        // Calculate the available space for the rectangle with 20dp padding
        val padding = 20.dp.value * drawScope.density
        val availableWidth = canvasSize.width - (2 * padding)
        val availableHeight = canvasSize.height - (2 * padding) - 60.dp.value * drawScope.density // Space for labels

        // Calculate unit square size based on rectangle dimensions
        val unitSize = minOf(
            availableWidth / length.toFloat(),
            availableHeight / width.toFloat()
        )
        
        // Calculate actual rectangle size
        val rectWidth = unitSize * length.toFloat()
        val rectHeight = unitSize * width.toFloat()
        
        // Left-center align the rectangle
        val startX = padding
        val startY = (canvasSize.height - rectHeight) / 2f + 30.dp.value * drawScope.density // Leave space for top labels

        val textPaint = Paint().apply {
            color = baseWhite.toArgb()
            textSize = 14.dp.value * drawScope.density
            textAlign = Paint.Align.CENTER
        }
        
        val dimensionPaint = Paint().apply {
            color = focusAmber.toArgb()
            textSize = 16.dp.value * drawScope.density
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        
        // Draw the outer rectangle outline
        drawScope.drawRect(
            color = baseWhite,
            topLeft = androidx.compose.ui.geometry.Offset(startX, startY),
            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.value * drawScope.density)
        )
        
        // Draw dimension labels
        // Length label (bottom)
        drawScope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "$lengthLabel: $length",
                startX + rectWidth/2,
                startY + rectHeight + 30.dp.value * drawScope.density,
                dimensionPaint
            )
        }
        
        // Width label (left side, rotated)
        drawScope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(startX - 25.dp.value * drawScope.density, startY + rectHeight/2)
            canvas.nativeCanvas.rotate(-90f)
            canvas.nativeCanvas.drawText(
                "$widthLabel: $width",
                0f,
                0f,
                dimensionPaint
            )
            canvas.nativeCanvas.restore()
        }
        
        // Show area calculation
        val totalArea = length * width
        val areaText = "Área = $length × $width = $totalArea unidades cuadradas"
        
        drawScope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                areaText,
                canvasSize.width / 2f,
                startY - 5.dp.value * drawScope.density,
                textPaint
            )
        }
    }
}
