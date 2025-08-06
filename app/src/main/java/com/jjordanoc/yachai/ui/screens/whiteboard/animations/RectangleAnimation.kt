package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import java.util.UUID
import com.jjordanoc.yachai.ui.screens.whiteboard.model.AnimationCommand
import com.jjordanoc.yachai.ui.theme.*

/**
 * Animation for drawing a rectangle with dimensions and labels.
 * The rectangle is displayed in a card-like container.
 */
class RectangleAnimation(
    val length: Int,
    val width: Int,
    val lengthLabel: String = "longitud",
    val widthLabel: String = "ancho",
    override val id: String = UUID.randomUUID().toString()
) : MathAnimation {
    
    // Educational color hierarchy for whiteboard

    
    @Composable
    override fun draw() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = baseWhite.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rectangle canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val canvasSize = size
                    
                    // Calculate the available space for the rectangle
                    val padding = 20.dp.toPx()
                    val availableWidth = canvasSize.width - (2 * padding)
                    val availableHeight = canvasSize.height - (2 * padding) - 30.dp.toPx()
                    
                    // Calculate unit square size based on rectangle dimensions
                    val unitSize = minOf(
                        availableWidth / length.toFloat(),
                        availableHeight / width.toFloat()
                    )
                    
                    // Calculate actual rectangle size
                    val rectWidth = unitSize * length.toFloat()
                    val rectHeight = unitSize * width.toFloat()
                    
                    // Center the rectangle
                    val startX = (canvasSize.width - rectWidth) / 2f
                    val startY = (canvasSize.height - rectHeight) / 2f
                    
                    val textPaint = Paint().apply {
                        color = baseWhite.toArgb()
                        textSize = 12.dp.toPx()
                        textAlign = Paint.Align.CENTER
                    }
                    
                    val dimensionPaint = Paint().apply {
                        color = focusAmber.toArgb()
                        textSize = 14.dp.toPx()
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    
                    // Draw the outer rectangle outline
                    drawRect(
                        color = baseWhite,
                        topLeft = androidx.compose.ui.geometry.Offset(startX, startY),
                        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    
                    // Draw dimension labels
                    // Length label (bottom)
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            "$lengthLabel: $length",
                            startX + rectWidth/2,
                            startY + rectHeight + 20.dp.toPx(),
                            dimensionPaint
                        )
                    }
                    
                    // Width label (left side, rotated)
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.translate(startX - 15.dp.toPx(), startY + rectHeight/2)
                        canvas.nativeCanvas.rotate(-90f)
                        canvas.nativeCanvas.drawText(
                            "$widthLabel: $width",
                            0f,
                            0f,
                            dimensionPaint
                        )
                        canvas.nativeCanvas.restore()
                    }
                }
            }
        }
    }
    
    companion object {
        /**
         * Static signature for this animation type
         */
        val signature = AnimationSignature(
            name = "drawRectangle",
            description = "Dibuja un rectángulo con dimensiones especificadas",
            args = mapOf(
                "length" to "número (largo)",
                "width" to "número (ancho)",
            )
        )
        
        /**
         * Create a RectangleAnimation from a command
         */
        fun fromCommand(command: AnimationCommand): RectangleAnimation? {
            // Handle new string-based base/height parameters
            val baseStr = command.args.base
            val heightStr = command.args.height
            
            // Also support legacy numeric parameters
            val lengthNum = command.args.length
            val widthNum = command.args.width
            
            // Parse string values to integers
            val length = try {
                baseStr?.toInt() ?: lengthNum
            } catch (e: NumberFormatException) {
                lengthNum
            }
            
            val width = try {
                heightStr?.toInt() ?: widthNum
            } catch (e: NumberFormatException) {
                widthNum
            }
            
            val lengthLabel = command.args.lengthLabel ?: "longitud"
            val widthLabel = command.args.widthLabel ?: "ancho"
            
            return if (length != null && width != null && length > 0 && width > 0) {
                RectangleAnimation(
                    length = length,
                    width = width,
                    lengthLabel = lengthLabel,
                    widthLabel = widthLabel
                )
            } else {
                null
            }
        }
    }
}
