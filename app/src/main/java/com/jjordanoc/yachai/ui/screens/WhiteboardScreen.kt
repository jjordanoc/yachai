package com.jjordanoc.yachai.ui.screens

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import kotlin.math.sqrt
import android.speech.tts.TextToSpeech
import android.util.Log
import com.jjordanoc.yachai.utils.TAG

val mockJsonResponse = """
{
  "tutor_message": {
    "text": "Veo un triángulo rectángulo ABC con ángulo recto en el punto B. La hipotenusa AC mide 13 unidades y el lado AB mide 5 unidades. ¿Esa es la situación que estás tratando de resolver?",
    "anchor": "point:B",
    "position": "above"
  },
  "hint": "Recuerda que la hipotenusa es el lado más largo, opuesto al ángulo recto.",
  "animation": [
    {
      "command": "drawRightTriangle",
      "args": {
        "sideLengths": {
          "AC": "13",
          "AB": "5",
          "BC": "x"
        }
      }
    }
  ]
}
"""

private fun Offset.lerp(other: Offset, fraction: Float): Offset {
    return Offset(
        x = x + (other.x - x) * fraction,
        y = y + (other.y - y) * fraction
    )
}

@Composable
fun WhiteboardScreen(
    viewModel: WhiteboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentPath by remember { mutableStateOf(Path()) }
    var isDrawing by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        offset += panChange
    }

    val animationProgress = remember { Animatable(0f) }

    // --- Text-to-Speech Setup ---
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        Log.d(TAG, "Initializing TTS engine.")
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS Engine initialized successfully.")
                val result = tts?.setLanguage(Locale("es", "419")) // Latin American Spanish
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS language (es-419) not supported or missing data.")
                } else {
                    Log.d(TAG, "TTS language set to Latin American Spanish.")
                    ttsInitialized = true
                }
            } else {
                Log.e(TAG, "TTS Engine initialization failed with status: $status")
            }
        }
        onDispose {
            Log.d(TAG, "Shutting down TTS engine.")
            tts?.stop()
            tts?.shutdown()
        }
    }
    // --- End of TTS Setup ---

    // Trigger JSON processing and animation
    LaunchedEffect(Unit) {
        viewModel.processLlmResponse(mockJsonResponse)
    }

    // Find the triangle to animate from the state
    val animatedTriangle = uiState.items.filterIsInstance<WhiteboardItem.AnimatedTriangle>().firstOrNull()
    val tutorMessageText = animatedTriangle?.tutorMessage?.text

    LaunchedEffect(animatedTriangle) {
        if (animatedTriangle != null) {
            Log.d(TAG, "Starting whiteboard animation for triangle: $animatedTriangle")
            animationProgress.snapTo(0f) // Reset progress for new animations
            animationProgress.animateTo(
                targetValue = 3f, // 3 segments: AB, BC, AC
                animationSpec = tween(durationMillis = 3000, delayMillis = 500) // 1s per segment, 0.5s delay
            )
        }
    }

    // Trigger speech after animation has had time to complete
    LaunchedEffect(tutorMessageText, ttsInitialized) {
        if (tutorMessageText != null && ttsInitialized) {
            // Wait for visual animation to finish before speaking
            kotlinx.coroutines.delay(3500)
            Log.d(TAG, "Triggering TTS speech for: '$tutorMessageText'")
            tts?.speak(tutorMessageText, TextToSpeech.QUEUE_FLUSH, null, "tutor_message")
        }
    }

    Scaffold(
        bottomBar = {
            WhiteboardBottomBar(
                text = uiState.textInput,
                onTextChange = viewModel::onTextInputChanged,
                onSendClick = viewModel::onSendText,
                onImageClick = { /* TODO: Implement image picking */ }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .clipToBounds()
                .transformable(state = transformState)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                /*
                // Commented out drawing logic as requested
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val transformedPosition = (position - offset) / scale
                            currentPath.moveTo(transformedPosition.x, transformedPosition.y)
                            isDrawing = true
                        },
                        onDragEnd = {
                            viewModel.addPath(currentPath, Color.Black, 5f)
                            currentPath = Path()
                            isDrawing = false
                        },
                        onDragCancel = {
                            currentPath = Path()
                            isDrawing = false
                        }
                    ) { change, _ ->
                        val transformedPosition = (change.position - offset) / scale
                        currentPath.lineTo(transformedPosition.x, transformedPosition.y)
                        // Create a new path object to trigger recomposition
                        currentPath = Path().apply { addPath(currentPath) }
                    }
                }
                */
            ) {
                if (animatedTriangle != null) {
                    // --- Figure and Text Calculation Phase ---

                    // 1. Define relative points for the triangle
                    val pA = animatedTriangle.b + animatedTriangle.a
                    val pB = animatedTriangle.b
                    val pC = animatedTriangle.b + animatedTriangle.c

                    // 2. Calculate the bounding box of the triangle figure
                    val figureLeft = listOf(pA.x, pB.x, pC.x).minOrNull() ?: 0f
                    val figureTop = listOf(pA.y, pB.y, pC.y).minOrNull() ?: 0f
                    val figureRight = listOf(pA.x, pB.x, pC.x).maxOrNull() ?: 0f
                    val figureBottom = listOf(pA.y, pB.y, pC.y).maxOrNull() ?: 0f
                    val figureCenter = Offset((figureLeft + figureRight) / 2, (figureTop + figureBottom) / 2)

                    // 3. Prepare tutor message text and paint
                    val tutorMessageData = animatedTriangle.tutorMessage
                    val tutorMessageText = tutorMessageData.text
                    val tutorPaint = Paint().apply {
                        color = Color.Black.toArgb()
                        textSize = 35f
                        textAlign = Paint.Align.LEFT // Right-align text
                    }
                    val lines = tutorMessageText.split(" ").chunked(6).map { it.joinToString(" ") }
                    val fontMetrics = tutorPaint.fontMetrics
                    val textHeight = lines.size * tutorPaint.fontSpacing
                    val textWidth = lines.map { tutorPaint.measureText(it) }.maxOrNull() ?: 0f

                    // 4. Determine text position based on figure center
                    val padding = 80f
                    val textRightX: Float
                    val topOfTextBlockY: Float

                    when (tutorMessageData.position) {
                        "above" -> {
                            textRightX = figureCenter.x + textWidth / 2
                            topOfTextBlockY = figureTop - padding - textHeight
                        }
                        // Add other positions like "below", "left", "right" if needed
                        else -> { // Default to "above"
                            textRightX = figureCenter.x + textWidth / 2
                            topOfTextBlockY = figureTop - padding - textHeight
                        }
                    }

                    // 5. Calculate the final translation offset to center everything
                    val totalLeft = listOf(figureLeft, textRightX - textWidth).minOrNull() ?: 0f
                    val totalTop = listOf(figureTop, topOfTextBlockY).minOrNull() ?: 0f
                    val totalRight = listOf(figureRight, textRightX).maxOrNull() ?: 0f
                    val totalBottom = listOf(figureBottom, topOfTextBlockY + textHeight).maxOrNull() ?: 0f
                    val totalWidth = totalRight - totalLeft
                    val totalHeight = totalBottom - totalTop
                    val canvasCenter = Offset(size.width / 2, size.height / 2)
                    val centeringOffset = canvasCenter - Offset(totalLeft + totalWidth / 2, totalTop + totalHeight / 2)

                    // --- Drawing Phase ---
                    translate(left = centeringOffset.x, top = centeringOffset.y) {
                        // Animate drawing the triangle sides
                        val progress = animationProgress.value

                        // Draw points A, B, C
                        drawCircle(Color.Red, radius = 8f, center = pA)
                        drawCircle(Color.Red, radius = 8f, center = pB)
                        drawCircle(Color.Red, radius = 8f, center = pC)

                        // Draw point labels
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val labelPaint = Paint().apply {
                                color = Color.Black.toArgb()
                                textSize = 40f
                                textAlign = Paint.Align.CENTER
                            }
                            nativeCanvas.drawText("A", pA.x, pA.y - 20f, labelPaint)
                            nativeCanvas.drawText("B", pB.x - 20f, pB.y + 15f, labelPaint)
                            nativeCanvas.drawText("C", pC.x + 20f, pC.y + 15f, labelPaint)
                        }


                        // Draw side AB
                        if (progress > 0) {
                            val lineProgress = progress.coerceAtMost(1f)
                            drawLine(Color.Blue, pB, pB.lerp(pA, lineProgress), strokeWidth = 5f)
                        }

                        // Draw side BC
                        if (progress > 1) {
                            val lineProgress = (progress - 1).coerceAtMost(1f)
                            drawLine(Color.Blue, pB, pB.lerp(pC, lineProgress), strokeWidth = 5f)
                        }

                        // Draw side AC (hypotenuse)
                        if (progress > 2) {
                            val lineProgress = (progress - 2).coerceAtMost(1f)
                            drawLine(Color.Blue, pA, pA.lerp(pC, lineProgress), strokeWidth = 5f)
                        }

                        // Draw side length labels and tutor message
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val textPaint = Paint().apply {
                                color = Color.DarkGray.toArgb()
                                textSize = 35f
                                textAlign = Paint.Align.CENTER
                            }

                            val sideLengths = animatedTriangle.sideLengths
                            val midAB = pB.lerp(pA, 0.5f)
                            if (progress >= 1f) {
                                nativeCanvas.drawText(sideLengths.ab, midAB.x - 30f, midAB.y, textPaint)
                            }
                            val midBC = pB.lerp(pC, 0.5f)
                            if (progress >= 2f) {
                                nativeCanvas.drawText(sideLengths.bc, midBC.x, midBC.y + 40f, textPaint)
                            }
                            val midAC = pA.lerp(pC, 0.5f)
                            if (progress >= 3f) {
                                nativeCanvas.drawText(sideLengths.ac, midAC.x + 25f, midAC.y - 15f, textPaint)
                            }

                            // Draw tutor message
                            if (progress >= 3f) {
                                var currentY = topOfTextBlockY - fontMetrics.ascent // Get baseline of first line
                                for (line in lines) {
                                    nativeCanvas.drawText(line, textRightX, currentY, tutorPaint)
                                    currentY += tutorPaint.fontSpacing
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteboardBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onImageClick: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask a question or describe an image...") },
                maxLines = 5
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onImageClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Add Image"
                )
            }
        }
    }
}
