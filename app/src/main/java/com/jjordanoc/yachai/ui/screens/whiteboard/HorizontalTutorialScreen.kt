package com.jjordanoc.yachai.ui.screens.whiteboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.theme.TutorialGreen
import com.jjordanoc.yachai.ui.theme.TutorialTeal
import com.jjordanoc.yachai.ui.theme.TutorialGray
import com.jjordanoc.yachai.ui.theme.White
import kotlinx.coroutines.delay
import android.app.Application
import androidx.compose.foundation.border
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

import androidx.compose.runtime.DisposableEffect
import java.util.Locale
import android.util.Log
import com.jjordanoc.yachai.utils.TAG
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import com.jjordanoc.yachai.ui.screens.whiteboard.model.WhiteboardItem
import com.jjordanoc.yachai.ui.screens.whiteboard.model.RectanglePhase
import com.jjordanoc.yachai.ui.screens.whiteboard.model.GridPhase
import com.jjordanoc.yachai.ui.screens.whiteboard.animations.WhiteboardAutoFill

@Composable
fun HorizontalTutorialScreen(
    navController: NavController,
    viewModel: TutorialViewModel = viewModel(
        factory = TutorialViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val configuration = LocalConfiguration.current
    // App is always in landscape mode
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Navigation logic - handle back button to go to problem input
    LaunchedEffect(Unit) {
        // Set up back button handling
        // This ensures when user presses back from tutorial, they go to problem input
        // rather than the loading screen
    }
    
    // Animation for alpaca speaking - only animate when actually speaking
    val speakingAnimation = rememberInfiniteTransition(label = "alpaca_speaking")
    val isMouthOpen by if (uiState.isAlpacaSpeaking) {
        speakingAnimation.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mouth_animation"
        )
    } else {
        remember { Animatable(0f) }.asState()
    }
    
    // --- Text-to-Speech Setup ---
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
                    
                    // Set up TTS progress listener to sync with alpaca animation
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS started speaking: $utteranceId")
                            // Start alpaca speaking animation when TTS begins
                            viewModel.startAlpacaSpeaking()
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS finished speaking: $utteranceId")
                            // Signal alpaca finished speaking - enables "siguiente paso" button
                            viewModel.alpacaFinishedSpeaking()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS error for utterance: $utteranceId")
                            // Signal finished speaking even on error - enables "siguiente paso" button
                            viewModel.ttsFailed()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            Log.e(TAG, "TTS error for utterance: $utteranceId, code: $errorCode")
                            // Signal finished speaking even on error - enables "siguiente paso" button
                            viewModel.ttsFailed()
                        }
                    })
                    
                    ttsInitialized = true
                }
            } else {
                Log.e(TAG, "TTS Engine initialization failed with status: $status")
                // Trigger fallback when TTS fails to initialize
                viewModel.ttsFailed()
            }
        }
        onDispose {
            Log.d(TAG, "Shutting down TTS engine.")
            tts?.stop()
            tts?.shutdown()
        }
    }
    // --- End of TTS Setup ---

    // Trigger speech synchronized with alpaca speaking animation
    LaunchedEffect(uiState.tutorMessage, ttsInitialized, uiState.animationTrigger) {
        val tutorMessageText = uiState.tutorMessage
        if (tutorMessageText != null && ttsInitialized) {
            // Small delay to let UI update, then start TTS
            delay(500)
            
            // Use the tutor message directly for speech
            val speechText = tutorMessageText
            
            Log.d(TAG, "Triggering TTS speech for: '$speechText'")
            
            // Create unique utterance ID for tracking
            val utteranceId = "tutor_message_${System.currentTimeMillis()}"
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }
    

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(30.dp)
    ) {
        // Main whiteboard section with overlayed alpaca
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = TutorialGreen,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            // Content area with minimal padding and scrolling fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp, horizontal = 20.dp)
            ) {
                
                // Main content text - show different content based on state with scrolling fallback
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 80.dp, end = 200.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    item {
                                                Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            
                            // Show arithmetic animations (visual content only)
                                // Display number line if present
                                uiState.currentNumberLine?.let { numberLine ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                    ) {
                                        ArithmeticNumberLine(
                                            numberLine = numberLine,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Display expression if present  
                                uiState.currentExpression?.let { expression ->
                                    Text(
                                        text = expression,
                                        color = White,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Left,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Display animations using new grid system
                                if (uiState.activeAnimations.isNotEmpty()) {
                                    WhiteboardAutoFill(animations = uiState.activeAnimations)
                                }
                                


                            // Show error message if needed
                            if (uiState.showConfirmationFailureMessage) {
                                Text(
                                    text = "Por favor, vuelve a intentarlo escribiendo el problema en texto o subiendo una imagen.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Left,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
                

            }
            
            // Animated Alpaca at bottom right corner of whiteboard
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(
                        width = 200.dp,
                        height = 150.dp
                    )
            ) {
                // Determine which image to show based on speaking state from ViewModel
                val alpacaImage = if (uiState.isAlpacaSpeaking && isMouthOpen > 0.5f) {
                    R.drawable.alpakey_yap // Speaking/mouth open
                } else {
                    R.drawable.alpakey // Normal/mouth closed
                }
                
                Image(
                    painter = painterResource(id = alpacaImage),
                    contentDescription = "Alpaca tutor",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        Spacer(modifier = Modifier.height(15.dp))
        
        // Tutorial control buttons
        Column(modifier = Modifier.padding(10.dp)) {
            // Show status indicator when tutor is speaking
            if (uiState.isAlpacaSpeaking) {
                Text(
                    text = "El tutor está hablando...",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            
            // Calculate available width for buttons (accounting for alpaca space)
            val alpacaWidth = 200.dp
            val availableWidth = LocalConfiguration.current.screenWidthDp.dp - 60.dp - alpacaWidth // Account for padding and alpaca
            
            Row(
                modifier = Modifier
                    .width(availableWidth)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    // "Siguiente paso" button
                    Button(
                        onClick = { viewModel.nextStepButtonHandler() },
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 140.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TutorialTeal,
                            contentColor = White,
                            disabledContainerColor = TutorialGray,
                            disabledContentColor = Color.Gray
                        ),
                        enabled = !uiState.isAlpacaSpeaking && uiState.isInStepSequence && uiState.isReadyForNextStep
                    ) {
                        Text(
                            text = if (uiState.isProcessing) "Procesando..." else "Siguiente paso",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

// "Tengo una duda" button
                    OutlinedButton(
                        onClick = {
                            // TODO: Implement clarification functionality
                        },
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 140.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = TutorialTeal,
                            disabledContainerColor = TutorialGray,
                            disabledContentColor = Color.Gray
                        ),
                        border = BorderStroke(2.dp, TutorialTeal),
                        enabled = !uiState.isAlpacaSpeaking
                    ) {
                        Text(
                            text = "Tengo una duda",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

// "Repetir" button
                    OutlinedButton(
                        onClick = {
                            viewModel.repeatCurrentStep()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 140.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF666666),
                            disabledContainerColor = TutorialGray,
                            disabledContentColor = Color.LightGray
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (!uiState.isAlpacaSpeaking && uiState.tutorMessage?.isNotBlank() == true)
                                Color(0xFF666666) else Color.LightGray
                        ),
                        enabled = !uiState.isAlpacaSpeaking && uiState.tutorMessage?.isNotBlank() == true
                    ) {
                        Text(
                            text = "Repetir",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

 

@Composable
private fun ArithmeticNumberLine(
    numberLine: WhiteboardItem.AnimatedNumberLine,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawNumberLine(
            numberLine = numberLine,
            canvasSize = size
        )
    }
}

private fun DrawScope.drawNumberLine(
    numberLine: WhiteboardItem.AnimatedNumberLine,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    // Educational color hierarchy for whiteboard
    val baseWhite = Color(0xFFFFFBF0) // Level 1: Base objects (main shapes, number lines)
    val secondaryTeal = Color(0xFF4DB6AC) // Level 2: Secondary elements (grid lines, aux lines)
    val focusAmber = Color(0xFFFFC107) // Level 3: Focus & highlights (labels, measurements)
    val criticalYellow = Color(0xFFFFE082) // Level 4: Critical info (final answers, key results)
    
    val textPaint = Paint().apply {
        color = baseWhite.toArgb()
        textSize = 14.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    val highlightPaint = Paint().apply {
        color = criticalYellow.toArgb()
        textSize = 16.dp.toPx()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    val yPos = canvasSize.height / 2f
    val tickHeight = 6.dp.toPx()

    if (numberLine.marks.isEmpty()) {
        Log.w(TAG, "Cannot draw number line with no marks.")
        return
    }

    // Smart scaling: use available width efficiently
    val padding = 40.dp.toPx()
    val availableWidth = canvasSize.width - (2 * padding)
    val startX = padding
    val endX = canvasSize.width - padding

    // Find the actual min/max values that will be displayed (from marks, not range)
    val displayedMarks = numberLine.marks.sorted()
    val minDisplayed = displayedMarks.first()
    val maxDisplayed = displayedMarks.last()
    val displaySpan = maxDisplayed - minDisplayed
    
    // Draw main line in base white
    drawLine(
        color = baseWhite,
        start = androidx.compose.ui.geometry.Offset(startX, yPos),
        end = androidx.compose.ui.geometry.Offset(endX, yPos),
        strokeWidth = 2.dp.toPx()
    )

    fun getXForValue(value: Int): Float {
        // Proportional positioning based on the displayed range, not the full range
        return if (displaySpan == 0) {
            // If all marks are the same value, center it
            startX + availableWidth / 2f
        } else {
            val normalized = (value - minDisplayed).toFloat() / displaySpan
            startX + normalized * availableWidth
        }
    }

    // Draw marks and labels (only the specified marks, not every number in range)
    for (markValue in displayedMarks) {
        val x = getXForValue(markValue)
        drawLine(
            color = baseWhite,
            start = androidx.compose.ui.geometry.Offset(x, yPos - tickHeight),
            end = androidx.compose.ui.geometry.Offset(x, yPos + tickHeight),
            strokeWidth = 1.5.dp.toPx()
        )
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                markValue.toString(), 
                x, 
                yPos + tickHeight + 20.dp.toPx(), 
                textPaint
            )
        }
    }

    // Draw highlights (only for values that are actually marked)
    numberLine.highlight.forEach { highlightValue ->
        if (highlightValue in displayedMarks) {
            val x = getXForValue(highlightValue)
            // Draw a circle for the highlight
            drawCircle(
                color = criticalYellow,
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, yPos)
            )
            // Overwrite the label with a highlighted one
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    highlightValue.toString(), 
                    x, 
                    yPos + tickHeight + 20.dp.toPx(), 
                    highlightPaint
                )
            }
        }
    }
}
