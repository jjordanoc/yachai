package com.jjordanoc.yachai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
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
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
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
    
    // Image picker setup
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract(),
        onResult = { result ->
            if (result.isSuccessful) {
                viewModel.onImageSelected(result.uriContent)
            }
        }
    )

    fun launchImageCropper() {
        cropImageLauncher.launch(
            CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeGallery = true,
                    imageSourceIncludeCamera = true
                )
            )
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                launchImageCropper()
            } else {
                Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
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
                            // Stop alpaca speaking animation when TTS ends
                            viewModel.stopAlpacaSpeaking()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS error for utterance: $utteranceId")
                            viewModel.stopAlpacaSpeaking()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            Log.e(TAG, "TTS error for utterance: $utteranceId, code: $errorCode")
                            viewModel.stopAlpacaSpeaking()
                        }
                    })
                    
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

    // Trigger speech synchronized with alpaca speaking animation
    LaunchedEffect(uiState.tutorMessage, ttsInitialized, uiState.flowState) {
        val tutorMessageText = uiState.tutorMessage
        if (tutorMessageText != null && ttsInitialized) {
            // Small delay to let UI update, then start TTS
            delay(500)
            
            // Prepare the speech text based on the current flow state
            val speechText = when (uiState.flowState) {
                TutorialFlowState.INTERPRETING -> {
                    "Veamos si entendí correctamente. El problema que quieres resolver es $tutorMessageText"
                }
                else -> tutorMessageText
            }
            
            Log.d(TAG, "Triggering TTS speech for: '$speechText'")
            
            // Create unique utterance ID for tracking
            val utteranceId = "tutor_message_${System.currentTimeMillis()}"
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }
    
    // Show loading screen if model is loading
    if (uiState.isModelLoading) {
        LoadingScreen()
        return
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
            // Content area with proper padding
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 60.dp, horizontal = 20.dp)
            ) {
                // Left navigation button
                IconButton(
                    onClick = { 
                        // Handle left navigation and trigger alpaca speaking
                        viewModel.triggerAlpacaSpeaking()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(50.dp)
                        .alpha(0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Main content text - show different content based on state
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 80.dp, end = if (isLandscape) 200.dp else 80.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    when (uiState.flowState) {
                        TutorialFlowState.INITIAL -> {
                            Text(
                                text = "Un número es divisible por 5 si termina en 0 o 5.",
                                color = White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Cursive,
                                textAlign = TextAlign.Left,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Un número es divisible por 25 si termina en 00, 25, 50 o 75.",
                                color = White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Cursive,
                                textAlign = TextAlign.Left,
                                lineHeight = 28.sp
                            )
                        }
                        TutorialFlowState.INTERPRETING -> {
                            Text(
                                text = uiState.tutorMessage ?: "Analizando tu problema...",
                                color = White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Cursive,
                                textAlign = TextAlign.Left,
                                lineHeight = 28.sp
                            )
                        }
                        TutorialFlowState.AWAITING_CONFIRMATION -> {
                            Text(
                                text = uiState.tutorMessage ?: "¿Es esto correcto?",
                                color = White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Cursive,
                                textAlign = TextAlign.Left,
                                lineHeight = 28.sp
                            )
                        }
                        TutorialFlowState.CHATTING -> {
                            Text(
                                text = uiState.tutorMessage ?: "¡Sigamos aprendiendo!",
                                color = White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Cursive,
                                textAlign = TextAlign.Left,
                                lineHeight = 28.sp
                            )
                        }
                    }
                    
                    // Show arithmetic animations below text
                    if (uiState.flowState == TutorialFlowState.CHATTING) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
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
                        }
                        
                        // Display expression if present  
                        uiState.currentExpression?.let { expression ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = expression,
                                color = White,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Show error message if needed
                    if (uiState.showConfirmationFailureMessage) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Por favor, vuelve a intentarlo escribiendo el problema en texto o subiendo una imagen.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Left,
                            lineHeight = 22.sp
                        )
                    }
                }
                
                // Right navigation button
                IconButton(
                    onClick = { 
                        // Handle right navigation and trigger alpaca speaking
                        viewModel.triggerAlpacaSpeaking()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(50.dp)
                        .alpha(0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            // Animated Alpaca overlayed on bottom right corner of whiteboard
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(
                        width = if (isLandscape) 200.dp else 120.dp,
                        height = if (isLandscape) 150.dp else 100.dp
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
            
            // Confirmation buttons overlay (shown during AWAITING_CONFIRMATION state)
            if (uiState.flowState == TutorialFlowState.AWAITING_CONFIRMATION) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reject button (X)
                    FloatingActionButton(
                        onClick = { viewModel.onConfirmationReject() },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reject",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Accept button (✓)
                    FloatingActionButton(
                        onClick = { viewModel.onConfirmationAccept() },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(15.dp))
        
        // Chat interface - only show during appropriate states
        if (uiState.flowState != TutorialFlowState.AWAITING_CONFIRMATION) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Show selected image if any
                uiState.selectedImageUri?.let { uri ->
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                            launchImageCropper()
                                        }
                                        else -> {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                }
                        )
                        IconButton(
                            onClick = { viewModel.onImageSelected(null) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Text input field
                    OutlinedTextField(
                        value = uiState.textInput,
                        onValueChange = viewModel::onTextInputChanged,
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TutorialGray,
                            unfocusedContainerColor = TutorialGray,
                            disabledContainerColor = TutorialGray,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        placeholder = {
                            Text(
                                text = when (uiState.flowState) {
                                    TutorialFlowState.INITIAL -> "Describe tu problema de matemáticas..."
                                    TutorialFlowState.INTERPRETING -> "Analizando..."
                                    TutorialFlowState.CHATTING -> "Escribe tu respuesta..."
                                    else -> "Escribe aquí..."
                                },
                                color = Color.Gray
                            )
                        },
                        enabled = uiState.flowState != TutorialFlowState.INTERPRETING
                    )
                    
                    // Image selection button
                    FloatingActionButton(
                        onClick = {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                    launchImageCropper()
                                }
                                else -> {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        containerColor = TutorialTeal,
                        contentColor = White
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Send button
                    val sendEnabled = uiState.textInput.isNotBlank() || uiState.selectedImageUri != null
                    FloatingActionButton(
                        onClick = { 
                            if (sendEnabled) {
                                viewModel.onSendText()
                            }
                        },
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        containerColor = if (sendEnabled) TutorialTeal else TutorialGray,
                        contentColor = if (sendEnabled) White else Color.Gray
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Microphone button
                    FloatingActionButton(
                        onClick = { 
                            // Handle microphone action - trigger alpaca speaking for now
                            viewModel.triggerAlpacaSpeaking() 
                        },
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        containerColor = TutorialTeal,
                        contentColor = White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing)
        ),
        label = "rotation"
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(64.dp)
                    .graphicsLayer { rotationZ = rotation },
                strokeWidth = 6.dp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Preparando la pizarra y repasando fórmulas…",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
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
    // Chalk colors for authentic chalkboard look
    val chalkWhite = Color(0xFFF5F5DC) // Slightly off-white like real chalk
    val chalkRed = Color(0xFFDC143C) // Classic red chalk color
    
    val textPaint = Paint().apply {
        color = chalkWhite.toArgb()
        textSize = 14.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    val highlightPaint = Paint().apply {
        color = chalkRed.toArgb()
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
    
    // Draw main line in chalk white
    drawLine(
        color = chalkWhite,
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
            color = chalkWhite,
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
            // Draw a circle for the highlight in red chalk
            drawCircle(
                color = chalkRed,
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