package com.jjordanoc.yachai.ui.screens.whiteboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.jjordanoc.yachai.ui.Routes
import com.jjordanoc.yachai.ui.theme.TutorialGreen
import com.jjordanoc.yachai.ui.theme.TutorialTeal
import com.jjordanoc.yachai.ui.theme.TutorialGray
import com.jjordanoc.yachai.ui.theme.White
import kotlinx.coroutines.delay
import android.app.Application
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.graphicsLayer
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
                                
                                // Display rectangle area animation if present
                                uiState.currentRectangle?.let { rectangle ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    ) {
                                        AnimatedRectangleComponent(
                                            rectangle = rectangle,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Display grid animation if present (overlays on rectangle)
                                uiState.currentGrid?.let { grid ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    ) {
                                        AnimatedGridComponent(
                                            grid = grid,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Display data visualizations vertically
                                uiState.currentDataTable?.let { table ->
                                    DataTableComponent(
                                        table = table,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                uiState.currentTallyChart?.let { tally ->
                                    TallyChartComponent(
                                        tallyChart = tally,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                uiState.currentBarChart?.let { barChart ->
                                    BarChartComponent(
                                        barChart = barChart,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                uiState.currentPieChart?.let { pieChart ->
                                    PieChartComponent(
                                        pieChart = pieChart,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                uiState.currentDotPlot?.let { dotPlot ->
                                    DotPlotComponent(
                                        dotPlot = dotPlot,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                uiState.currentDataSummary?.let { summary ->
                                    DataSummaryComponent(
                                        dataSummary = summary,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
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
            
            // Animated Alpaca overlayed on bottom right corner of whiteboard
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
                // Control buttons for automatic explanation flow accounting for alpaca width
                val alpacaWidth = 200.dp // Always landscape mode
                val availableWidth = LocalConfiguration.current.screenWidthDp.dp - 20.dp - alpacaWidth // Account for padding and alpaca
                
                Row(
                    modifier = Modifier
                        .width(availableWidth)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Siguiente paso" button - Keep as is (this one looks good)
                    Button(
                        onClick = { viewModel.proceedToNextStep() },
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
                        enabled = uiState.isInStepSequence && uiState.isReadyForNextStep
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

// "Tengo una duda" button - Add border and improve contrast
                    OutlinedButton(  // Use OutlinedButton instead of Button
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
                        border = BorderStroke(2.dp, TutorialTeal) // Add teal border
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

// "Repetir" button - Better contrast and border
                    OutlinedButton(  // Use OutlinedButton for consistency
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
                            contentColor = Color(0xFF666666), // Darker gray for better contrast
                            disabledContainerColor = TutorialGray,
                            disabledContentColor = Color.LightGray
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (uiState.tutorMessage?.isNotBlank() == true && !uiState.isAlpacaSpeaking)
                                Color(0xFF666666) else Color.LightGray
                        ),
                        enabled = uiState.tutorMessage?.isNotBlank() == true && !uiState.isAlpacaSpeaking
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


                    // "Nuevo Problema" button
//                    Button(
//                        onClick = {
//                            // Reset state and navigate back to problem input
//                            viewModel.resetForNewProblem()
//                            navController.navigate(Routes.PROBLEM_INPUT_SCREEN) {
//                                // Clear the entire back stack
//                                popUpTo(Routes.MAIN_SCREEN) { inclusive = false }
//                            }
//                        },
//                        modifier = Modifier
//                            .weight(1f)
//                            .widthIn(max = 140.dp)
//                            .height(52.dp),
//                        shape = RoundedCornerShape(16.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color.White,
//                            contentColor = TutorialTeal,
//                            disabledContainerColor = TutorialGray,
//                            disabledContentColor = Color.Gray
//                        )
//                    ) {
//                        Text(
//                            text = "Nuevo Problema",
//                            fontSize = 15.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            textAlign = TextAlign.Center,
//                            maxLines = 2,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
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

// Data Visualization Components
@Composable
private fun DataTableComponent(
    table: WhiteboardItem.DataTable,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Headers with overflow protection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            table.headers.forEach { header ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = header,
                        color = White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Rows with overflow protection
        table.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEachIndexed { index, cell ->
                    if (index < table.headers.size) { // Ensure we don't exceed header count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Text(
                                text = cell,
                                color = White,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

@Composable
private fun TallyChartComponent(
    tallyChart: WhiteboardItem.TallyChart,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        tallyChart.categories.forEachIndexed { index, category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category name with overflow protection
                Text(
                    text = category,
                    color = White,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(0.3f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Draw tally marks with proper spacing
                val count = tallyChart.counts.getOrNull(index) ?: 0
                val tallyText = buildString {
                    repeat(count / 5) { append("㸧 ") } // Groups of 5
                    repeat(count % 5) { append("| ") } // Individual marks
                }
                
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .background(
                            color = White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = tallyText.ifEmpty { "—" },
                        color = Color(0xFFFFE4B5), // Light yellow for tally marks
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                
                // Count number
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .background(
                            color = White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = count.toString(),
                        color = Color(0xFF87CEEB), // Light blue for count
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (index < tallyChart.categories.size - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun BarChartComponent(
    barChart: WhiteboardItem.BarChart,
    modifier: Modifier = Modifier
) {
    if (barChart.labels.isEmpty() || barChart.values.isEmpty()) {
        Box(modifier = modifier) {
            Text(
                text = "Gráfico de Barras (sin datos)",
                color = White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawBarChart(
            labels = barChart.labels,
            values = barChart.values,
            highlightedIndex = barChart.highlightedIndex,
            canvasSize = size
        )
    }
}

@Composable
private fun PieChartComponent(
    pieChart: WhiteboardItem.PieChart,
    modifier: Modifier = Modifier
) {
    if (pieChart.labels.isEmpty() || pieChart.values.isEmpty()) {
        Box(modifier = modifier) {
            Text(
                text = "Gráfico Circular (sin datos)",
                color = White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawPieChart(
            labels = pieChart.labels,
            values = pieChart.values,
            highlightedIndex = pieChart.highlightedIndex,
            canvasSize = size
        )
    }
}

@Composable
private fun DotPlotComponent(
    dotPlot: WhiteboardItem.DotPlot,
    modifier: Modifier = Modifier
) {
    if (dotPlot.values.isEmpty()) {
        Box(modifier = modifier) {
            Text(
                text = "Diagrama de Puntos (sin datos)",
                color = White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawDotPlot(
            values = dotPlot.values,
            min = dotPlot.min,
            max = dotPlot.max,
            highlightedIndices = dotPlot.highlightedIndices,
            canvasSize = size
        )
    }
}

@Composable
private fun DataSummaryComponent(
    dataSummary: WhiteboardItem.DataSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = dataSummary.summary,
            color = White,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        dataSummary.meanValue?.let { mean ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Promedio: ",
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format("%.1f", mean),
                    color = Color(0xFFFFE4B5), // Light yellow
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (dataSummary.rangeMin != null && dataSummary.rangeMax != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Rango: ",
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${dataSummary.rangeMin} - ${dataSummary.rangeMax}",
                    color = Color(0xFF87CEEB), // Light blue
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Canvas drawing functions for charts
private fun DrawScope.drawBarChart(
    labels: List<String>,
    values: List<Int>,
    highlightedIndex: Int?,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    if (labels.isEmpty() || values.isEmpty()) return
    
    val maxValue = values.maxOrNull()?.toFloat() ?: 1f
    val barCount = minOf(labels.size, values.size)
    val barWidth = (canvasSize.width * 0.8f) / barCount
    val barSpacing = barWidth * 0.2f
    val chartHeight = canvasSize.height * 0.7f
    val baseY = canvasSize.height * 0.85f
    
    // Educational color hierarchy for whiteboard
    val baseWhite = Color(0xFFFFFBF0) // Level 1: Base objects
    val secondaryTeal = Color(0xFF4DB6AC) // Level 2: Secondary elements  
    val focusAmber = Color(0xFFFFC107) // Level 3: Focus & highlights
    
    val textPaint = Paint().apply {
        color = baseWhite.toArgb()
        textSize = 10.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    
    repeat(barCount) { index ->
        val value = values.getOrNull(index) ?: 0
        val label = labels.getOrNull(index) ?: ""
        val isHighlighted = highlightedIndex == index
        
        val barHeight = (value.toFloat() / maxValue) * chartHeight
        val barX = (canvasSize.width * 0.1f) + (index * (barWidth + barSpacing))
        val barY = baseY - barHeight
        
        // Draw bar
        val barColor = if (isHighlighted) focusAmber else secondaryTeal
        drawRect(
            color = barColor,
            topLeft = androidx.compose.ui.geometry.Offset(barX, barY),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
        
        // Draw value on top of bar
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                value.toString(),
                barX + barWidth/2,
                barY - 5.dp.toPx(),
                textPaint
            )
        }
        
        // Draw label below bar (truncate if too long)
        val truncatedLabel = if (label.length > 8) "${label.take(6)}.." else label
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                truncatedLabel,
                barX + barWidth/2,
                baseY + 15.dp.toPx(),
                textPaint
            )
        }
    }
}

private fun DrawScope.drawPieChart(
    labels: List<String>,
    values: List<Int>,
    highlightedIndex: Int?,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    if (labels.isEmpty() || values.isEmpty()) return
    
    val total = values.sum().toFloat()
    if (total <= 0) return
    
    val centerX = canvasSize.width / 2f
    val centerY = canvasSize.height / 2f
    val radius = minOf(centerX, centerY) * 0.7f
    
    // Educational color hierarchy - soft, varied palette
    val colors = listOf(
        Color(0xFF4DB6AC), // Muted teal (Level 2)
        Color(0xFFB0BEC5), // Soft blue-gray (Level 2)  
        Color(0xFFFFC107), // Warm amber (Level 3)
        Color(0xFFFF7043), // Gentle orange (Level 3)
        Color(0xFF81C784), // Soft green
        Color(0xFFBA68C8)  // Soft purple
    )
    
    val baseWhite = Color(0xFFFFFBF0) // Level 1: Base objects
    val textPaint = Paint().apply {
        color = baseWhite.toArgb()
        textSize = 10.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    
    var currentAngle = -90f // Start from top
    
    repeat(minOf(labels.size, values.size)) { index ->
        val value = values.getOrNull(index) ?: 0
        val label = labels.getOrNull(index) ?: ""
        val percentage = (value.toFloat() / total) * 100f
        val sweepAngle = (value.toFloat() / total) * 360f
        
        val isHighlighted = highlightedIndex == index
        val sliceRadius = if (isHighlighted) radius * 1.1f else radius
        
        val color = colors[index % colors.size]
        
        // Draw pie slice
        drawArc(
            color = color,
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(
                centerX - sliceRadius,
                centerY - sliceRadius
            ),
            size = androidx.compose.ui.geometry.Size(sliceRadius * 2, sliceRadius * 2)
        )
        
        // Draw label at the middle of the slice
        if (percentage >= 5f) { // Only show label if slice is large enough
            val labelAngle = Math.toRadians((currentAngle + sweepAngle / 2).toDouble())
            val labelX = centerX + (sliceRadius * 0.7f * kotlin.math.cos(labelAngle)).toFloat()
            val labelY = centerY + (sliceRadius * 0.7f * kotlin.math.sin(labelAngle)).toFloat()
            
            val truncatedLabel = if (label.length > 6) "${label.take(4)}.." else label
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    truncatedLabel,
                    labelX,
                    labelY - 5.dp.toPx(),
                    textPaint
                )
                canvas.nativeCanvas.drawText(
                    "${percentage.toInt()}%",
                    labelX,
                    labelY + 10.dp.toPx(),
                    textPaint
                )
            }
        }
        
        currentAngle += sweepAngle
    }
}

private fun DrawScope.drawDotPlot(
    values: List<Int>,
    min: Int,
    max: Int,
    highlightedIndices: List<Int>,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    if (values.isEmpty() || max <= min) return
    
    val range = max - min
    val dotRadius = 3.dp.toPx()
    val baseY = canvasSize.height * 0.7f
    val plotWidth = canvasSize.width * 0.8f
    val plotStartX = canvasSize.width * 0.1f
    
    // Educational color hierarchy for whiteboard
    val baseWhite = Color(0xFFFFFBF0) // Level 1: Base objects
    val secondaryTeal = Color(0xFF4DB6AC) // Level 2: Secondary elements  
    val focusAmber = Color(0xFFFFC107) // Level 3: Focus & highlights
    
    val textPaint = Paint().apply {
        color = baseWhite.toArgb()
        textSize = 10.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    
    // Draw axis line
    drawLine(
        color = baseWhite,
        start = androidx.compose.ui.geometry.Offset(plotStartX, baseY),
        end = androidx.compose.ui.geometry.Offset(plotStartX + plotWidth, baseY),
        strokeWidth = 1.dp.toPx()
    )
    
    // Draw scale marks and labels
    val markCount = minOf(11, range + 1) // Max 11 marks
    repeat(markCount) { i ->
        val value = min + (range.toFloat() / (markCount - 1) * i).toInt()
        val x = plotStartX + (plotWidth / (markCount - 1) * i)
        
        // Draw tick mark
        drawLine(
            color = baseWhite,
            start = androidx.compose.ui.geometry.Offset(x, baseY - 5.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(x, baseY + 5.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        
        // Draw label
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                value.toString(),
                x,
                baseY + 20.dp.toPx(),
                textPaint
            )
        }
    }
    
    // Count frequency of each value
    val valueCounts = values.groupingBy { it }.eachCount()
    
    // Draw dots
    valueCounts.forEach { (value, count) ->
        if (value in min..max) {
            val x = plotStartX + ((value - min).toFloat() / range) * plotWidth
            
            repeat(count) { stackIndex ->
                val y = baseY - 20.dp.toPx() - (stackIndex * dotRadius * 2.5f)
                val isHighlighted = highlightedIndices.contains(values.indexOf(value))
                val dotColor = if (isHighlighted) focusAmber else secondaryTeal
                
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun AnimatedRectangleComponent(
    rectangle: WhiteboardItem.AnimatedRectangle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawAnimatedRectangle(
            rectangle = rectangle,
            canvasSize = size
        )
    }
}

private fun DrawScope.drawAnimatedRectangle(
    rectangle: WhiteboardItem.AnimatedRectangle,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    // Educational color hierarchy for whiteboard
    val baseWhite = Color(0xFFFFFBF0) // Level 1: Base objects (main shapes)
    val secondaryTeal = Color(0xFF4DB6AC) // Level 2: Secondary elements (filled squares)
    val focusAmber = Color(0xFFFFC107) // Level 3: Focus & highlights (dimensions) 
    val criticalYellow = Color(0xFFFFE082) // Level 4: Critical info (highlights)
    
    // Calculate the available space for the rectangle
    val padding = 40.dp.toPx()
    val availableWidth = canvasSize.width - (2 * padding)
    val availableHeight = canvasSize.height - (2 * padding) - 60.dp.toPx() // Space for labels
    
    // Calculate unit square size based on rectangle dimensions
    val unitSize = minOf(
        availableWidth / rectangle.length,
        availableHeight / rectangle.width
    )
    
    // Calculate actual rectangle size
    val rectWidth = unitSize * rectangle.length
    val rectHeight = unitSize * rectangle.width
    
    // Center the rectangle
    val startX = (canvasSize.width - rectWidth) / 2f
    val startY = (canvasSize.height - rectHeight) / 2f + 30.dp.toPx() // Leave space for top labels
    
    val textPaint = Paint().apply {
        color = baseWhite.toArgb()
        textSize = 14.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    
    val dimensionPaint = Paint().apply {
        color = focusAmber.toArgb()
        textSize = 16.dp.toPx()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    
    // Phase 1: Always draw rectangle outline (including SETUP phase)
    // Draw the outer rectangle outline
    drawRect(
        color = baseWhite,
        topLeft = androidx.compose.ui.geometry.Offset(startX, startY),
        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )
    
    // Phase 2: Draw vertical grid lines
    if (rectangle.animationPhase == RectanglePhase.VERTICAL_LINES || rectangle.animationPhase == RectanglePhase.FILLING_ROWS) {
        // Draw vertical lines to show columns
        for (i in 1 until rectangle.length) {
            val x = startX + (i * unitSize)
            drawLine(
                color = baseWhite,
                start = androidx.compose.ui.geometry.Offset(x, startY),
                end = androidx.compose.ui.geometry.Offset(x, startY + rectHeight),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw horizontal lines to show rows
        for (i in 1 until rectangle.width) {
            val y = startY + (i * unitSize)
            drawLine(
                color = baseWhite,
                start = androidx.compose.ui.geometry.Offset(startX, y),
                end = androidx.compose.ui.geometry.Offset(startX + rectWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
    
    // Phase 3: Fill unit squares progressively
    if (rectangle.animationPhase == RectanglePhase.FILLING_ROWS) {
        for (row in 0 until rectangle.width) {
            for (col in 0 until rectangle.length) {
                // Only fill squares up to the current progress
                val shouldFill = row < rectangle.currentRow || 
                               (row == rectangle.currentRow && col <= rectangle.currentColumn)
                
                if (shouldFill) {
                    val squareX = startX + (col * unitSize)
                    val squareY = startY + (row * unitSize)
                    
                    // Fill the unit square
                    drawRect(
                        color = secondaryTeal.copy(alpha = 0.6f),
                        topLeft = androidx.compose.ui.geometry.Offset(squareX + 1.dp.toPx(), squareY + 1.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(unitSize - 2.dp.toPx(), unitSize - 2.dp.toPx())
                    )
                    
                    // Add a small dot in the center to emphasize it's a unit
                    drawCircle(
                        color = criticalYellow,
                        radius = 2.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            squareX + unitSize/2,
                            squareY + unitSize/2
                        )
                    )
                }
            }
        }
    }
    
    // Draw dimension labels if enabled
    if (rectangle.showDimensions) {
        // Length label (bottom)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "${rectangle.lengthLabel}: ${rectangle.length}",
                startX + rectWidth/2,
                startY + rectHeight + 30.dp.toPx(),
                dimensionPaint
            )
        }
        
        // Width label (left side, rotated)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(startX - 25.dp.toPx(), startY + rectHeight/2)
            canvas.nativeCanvas.rotate(-90f)
            canvas.nativeCanvas.drawText(
                "${rectangle.widthLabel}: ${rectangle.width}",
                0f,
                0f,
                dimensionPaint
            )
            canvas.nativeCanvas.restore()
        }
        
        // Show multiplication expression and current count
        val totalSquares = rectangle.length * rectangle.width
        val currentCount = if (rectangle.animationPhase == RectanglePhase.FILLING_ROWS) {
            rectangle.currentRow * rectangle.length + rectangle.currentColumn + 1
        } else {
            0
        }
        
        val expressionText = "${rectangle.length} × ${rectangle.width} = $totalSquares"
        val progressText = if (rectangle.animationPhase == RectanglePhase.FILLING_ROWS) {
            "Cuadrados llenados: $currentCount de $totalSquares"
        } else {
            "Área = $expressionText unidades cuadradas"
        }
        
        drawIntoCanvas { canvas ->
            if (rectangle.animationPhase == RectanglePhase.FILLING_ROWS) {
                canvas.nativeCanvas.drawText(
                    progressText,
                    canvasSize.width / 2f,
                    startY - 5.dp.toPx(),
                    textPaint
                )
            }
        }
    }
}

@Composable
private fun AnimatedGridComponent(
    grid: WhiteboardItem.AnimatedGrid,
    modifier: Modifier = Modifier
) {
    // Trigger automatic grid animation progression
    LaunchedEffect(grid.animationPhase) {
        if (grid.animationPhase == GridPhase.SETUP) {
            kotlinx.coroutines.delay(1000) // Wait 1s after setup
            // TODO: Trigger updateGrid command to move to GRID_LINES phase
        } else if (grid.animationPhase == GridPhase.GRID_LINES) {
            kotlinx.coroutines.delay(1000) // Wait 1s after grid lines
            // TODO: Trigger updateGrid command to start filling
        }
    }
    
    Canvas(modifier = modifier) {
        drawAnimatedGrid(
            grid = grid,
            canvasSize = size
        )
    }
}

private fun DrawScope.drawAnimatedGrid(
    grid: WhiteboardItem.AnimatedGrid,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    // Educational color hierarchy for whiteboard
    val baseWhite = Color(0xFFFFFBF0) // Level 1: Base objects (grid outline)
    val secondaryTeal = Color(0xFF4DB6AC) // Level 2: Secondary elements (grid lines)
    val focusAmber = Color(0xFFFFC107) // Level 3: Focus & highlights (dimensions)
    val criticalYellow = Color(0xFFFFE082) // Level 4: Critical info (unit labels)
    val softGreen = Color(0xFF81C784)  // Gentle green for progressive fill
    
    // Calculate the available space for the grid
    val padding = 40.dp.toPx()
    val availableWidth = canvasSize.width - (2 * padding)
    val availableHeight = canvasSize.height - (2 * padding) - 60.dp.toPx() // Space for labels
    
    // Calculate unit square size based on grid dimensions
    val unitSize = minOf(
        availableWidth / grid.length,
        availableHeight / grid.width
    )
    
    // Calculate actual grid size
    val gridWidth = unitSize * grid.length
    val gridHeight = unitSize * grid.width
    
    // Center the grid
    val startX = (canvasSize.width - gridWidth) / 2f
    val startY = (canvasSize.height - gridHeight) / 2f + 30.dp.toPx() // Leave space for top labels
    
    val textPaint = Paint().apply {
        color = baseWhite.toArgb()
        textSize = 12.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    
    val unitLabelPaint = Paint().apply {
        color = criticalYellow.toArgb()
        textSize = 10.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    
    // Phase 1: Show base rectangle outline (SETUP)
    if (grid.animationPhase != GridPhase.SETUP) {
        // Draw the outer grid outline
        drawRect(
            color = baseWhite,
            topLeft = androidx.compose.ui.geometry.Offset(startX, startY),
            size = androidx.compose.ui.geometry.Size(gridWidth, gridHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
    
    // Phase 2: Draw grid lines (GRID_LINES)
    if (grid.animationPhase == GridPhase.GRID_LINES || grid.animationPhase == GridPhase.FILLING_UNITS) {
        // Draw vertical lines to show unit columns
        for (i in 1 until grid.length) {
            val x = startX + (i * unitSize)
            drawLine(
                color = secondaryTeal,
                start = androidx.compose.ui.geometry.Offset(x, startY),
                end = androidx.compose.ui.geometry.Offset(x, startY + gridHeight),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw horizontal lines to show unit rows
        for (i in 1 until grid.width) {
            val y = startY + (i * unitSize)
            drawLine(
                color = secondaryTeal,
                start = androidx.compose.ui.geometry.Offset(startX, y),
                end = androidx.compose.ui.geometry.Offset(startX + gridWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw unit labels in each grid cell
        for (row in 0 until grid.width) {
            for (col in 0 until grid.length) {
                val cellX = startX + (col * unitSize) + unitSize/2
                val cellY = startY + (row * unitSize) + unitSize/2
                
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        grid.unit,
                        cellX,
                        cellY + 3.dp.toPx(), // Slight vertical offset
                        unitLabelPaint
                    )
                }
            }
        }
    }
    
    // Phase 3: Fill unit squares progressively (FILLING_UNITS)
    if (grid.animationPhase == GridPhase.FILLING_UNITS) {
        for (row in 0 until grid.width) {
            for (col in 0 until grid.length) {
                // Determine if this unit should be filled
                val shouldFill = row < grid.currentRow || 
                               (row == grid.currentRow && col < grid.currentColumn) ||
                               (row == grid.currentRow && col == grid.currentColumn && grid.fillProgress > 0f)
                
                if (shouldFill) {
                    val squareX = startX + (col * unitSize)
                    val squareY = startY + (row * unitSize)
                    
                    // For current unit being filled, show partial fill based on fillProgress
                    val isCurrentUnit = row == grid.currentRow && col == grid.currentColumn
                    val fillAmount = if (isCurrentUnit) grid.fillProgress else 1f
                    
                    if (fillAmount > 0f) {
                        // Smooth left-to-right fill animation
                        val fillWidth = unitSize * fillAmount
                        
                        drawRect(
                            color = softGreen.copy(alpha = 0.7f),
                            topLeft = androidx.compose.ui.geometry.Offset(squareX + 1.dp.toPx(), squareY + 1.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(fillWidth - 2.dp.toPx(), unitSize - 2.dp.toPx())
                        )
                        
                        // Add subtle border to filled units
                        if (fillAmount >= 1f) {
                            drawRect(
                                color = softGreen,
                                topLeft = androidx.compose.ui.geometry.Offset(squareX, squareY),
                                size = androidx.compose.ui.geometry.Size(unitSize, unitSize),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
        
        // Show area calculation progress
        val totalUnits = grid.length * grid.width
        val filledUnits = grid.currentRow * grid.length + grid.currentColumn + if (grid.fillProgress >= 1f) 1 else 0
        val progressText = "Área: $filledUnits de $totalUnits ${grid.unit}"
        
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                progressText,
                canvasSize.width / 2f,
                startY - 10.dp.toPx(),
                textPaint
            )
        }
    }
    
    // Show dimension labels
    if (grid.showDimensions) {
        // Length label (bottom)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "${grid.lengthLabel}: ${grid.length} unidades",
                startX + gridWidth/2,
                startY + gridHeight + 30.dp.toPx(),
                textPaint
            )
        }
        
        // Width label (left side, rotated)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(startX - 25.dp.toPx(), startY + gridHeight/2)
            canvas.nativeCanvas.rotate(-90f)
            canvas.nativeCanvas.drawText(
                "${grid.widthLabel}: ${grid.width} unidades",
                0f,
                0f,
                textPaint
            )
            canvas.nativeCanvas.restore()
        }
        
        // Show final area calculation
        val totalArea = grid.length * grid.width
        val areaText = "Área total = ${grid.length} × ${grid.width} = $totalArea ${grid.unit}"
        
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                areaText,
                canvasSize.width / 2f,
                startY + gridHeight + 50.dp.toPx(),
                textPaint
            )
        }
    }
} 