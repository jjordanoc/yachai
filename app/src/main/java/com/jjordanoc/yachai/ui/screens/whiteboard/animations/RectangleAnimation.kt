package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.jjordanoc.yachai.ui.screens.whiteboard.model.AnimationCommand
import com.jjordanoc.yachai.ui.theme.*
import kotlin.time.Duration

/**
 * Animation for drawing a rectangle with dimensions and labels.
 * The rectangle is displayed in a card-like container.
 */
class RectangleAnimation(
    val length: Int,
    val width: Int,
    val drawAreaGrid: Boolean = false,
    override val id: String = UUID.randomUUID().toString()
) : MathAnimation {
    // Global figure properties
    private val lengthLabel: String = "longitud"
    private val widthLabel: String = "ancho"
    private val gridStepDuration: Long = 500 // Duration for each grid step in milliseconds
    private val canvasHeight = 180.dp
    private val canvasWidth = 300.dp // Responsive width range

    // Educational color hierarchy for whiteboard

    
    @Composable
    override fun draw() {
        // Animation state for grid columns
        var visibleColumns by remember { mutableStateOf(0) }
        var fadeInAlpha by remember { mutableStateOf(0f) }
        var slideProgress by remember { mutableStateOf(0f) }
        
        val coroutineScope = rememberCoroutineScope()
        
        // Start grid animation when drawAreaGrid is true
        LaunchedEffect(drawAreaGrid) {
            if (drawAreaGrid) {
                // Initial delay before first column appears
                delay(gridStepDuration / 2)
                
                // First column fade-in animation
                visibleColumns = 1
                fadeInAlpha = 0f
                repeat(10) {
                    fadeInAlpha = (fadeInAlpha + 0.1f).coerceIn(0f, 1f)
                    delay(50) // 500ms total fade-in
                }
                
                // Subsequent columns with sliding animation
                for (col in 2..length) {
                    delay(gridStepDuration) // 2 second delay between columns
                    
                    visibleColumns = col
                    slideProgress = 0f
                    
                    // Sliding animation for current column
                    repeat(8) {
                        slideProgress = (slideProgress + 0.125f).coerceIn(0f, 1f)
                        delay(100) // 800ms total sliding animation
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .background(
                    color = baseWhite.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .width(canvasWidth) // Responsive width range
                .height(canvasHeight)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rectangle canvas
                Canvas(
                    modifier = Modifier
//                        .widthIn(min = 200.dp, max = 400.dp) // Responsive width range
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    val canvasSize = size
                    
                    // Calculate the available space for the rectangle
                    val padding = 20.dp.toPx()
                    val availableWidth = canvasSize.width - (2 * padding)
                    val availableHeight = canvasSize.height - (2 * padding)
                    
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
                    
                    // Draw grid if enabled
                    if (drawAreaGrid && visibleColumns > 0) {
                        for (col in 0 until visibleColumns) {
                            for (row in 0 until width) {
                                val squareX = startX + (col * unitSize)
                                val squareY = startY + (row * unitSize)
                                
                                // Calculate animation alpha for this column
                                val columnAlpha = when {
                                    col == 0 -> fadeInAlpha // First column uses fade-in
                                    col < visibleColumns - 1 -> 1f // Fully visible columns
                                    else -> slideProgress // Current sliding column
                                }.coerceIn(0f, 1f) // Clamp to valid range
                                
                                // Draw grid square with blue fill and darker border
                                drawRect(
                                    color = gridBlue.copy(alpha = columnAlpha),
                                    topLeft = androidx.compose.ui.geometry.Offset(squareX, squareY),
                                    size = androidx.compose.ui.geometry.Size(unitSize, unitSize)
                                )
                                
                                // Draw grid square border
                                drawRect(
                                    color = gridBorderBlue.copy(alpha = columnAlpha),
                                    topLeft = androidx.compose.ui.geometry.Offset(squareX, squareY),
                                    size = androidx.compose.ui.geometry.Size(unitSize, unitSize),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                    
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
    
    override fun toDescription(): String {
        val gridInfo = if (drawAreaGrid) " con cuadrícula de área" else ""
        return "Rectángulo de $length × $width unidades$gridInfo"
    }
    
    companion object {
        /**
         * Static signature for this animation type
         */
        val signature = AnimationSignature(
            command = "drawRectangle",
            description = "Dibuja un rectángulo con dimensiones especificadas",
            args = mapOf(
                "length" to "número (largo)",
                "width" to "número (ancho)",
                "drawAreaGrid" to "booleano [opcional] (divide el área rectángulo en unidades cuadradas para contar)",
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
            

            val drawAreaGrid = command.args.drawAreaGrid ?: false
            
            return if (length != null && width != null && length > 0 && width > 0) {
                RectangleAnimation(
                    length = length,
                    width = width,
                    drawAreaGrid = drawAreaGrid
                )
            } else {
                null
            }
        }
    }
}
