package com.jjordanoc.yachai.ui.screens

import android.graphics.Paint
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jjordanoc.yachai.utils.TAG
import java.util.Locale
import kotlin.math.sqrt
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.launch

/*
val mockJsonResponse = """
{
  "tutor_message": "Veo un triángulo rectángulo ABC con ángulo recto en el punto B. La hipotenusa AC mide 13 unidades y el lado AB mide 5 unidades. ¿Esa es la situación que estás tratando de resolver?",
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

val mockJsonResponse2 = """
{
  "tutor_message": "Muy bien. Sabemos que AB mide 5 y AC mide 13. El ángulo en B es recto. ¿Qué relación podríamos usar para encontrar la longitud del lado BC?",
  "hint": "Piensa en la relación que existe entre los lados de un triángulo rectángulo.",
  "animation": [
    { "command": "highlightAngle", "args": { "point": "B", "type": "right" } },
    { "command": "highlightSide", "args": { "segment": "AB" } },
    { "command": "highlightSide", "args": { "segment": "AC" } },
    { "command": "highlightSide", "args": { "segment": "BC", "label": "x" } }
  ]
}
"""
*/

private fun Offset.lerp(other: Offset, fraction: Float): Offset {
    return Offset(
        x = x + (other.x - x) * fraction,
        y = y + (other.y - y) * fraction
    )
}

@Composable
fun WhiteboardScreen(
    viewModel: WhiteboardViewModel = viewModel(
        factory = WhiteboardViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Log.d(TAG, "WhiteboardScreen recomposing with flowState: ${uiState.flowState}")

    when (uiState.flowState) {
        WhiteboardFlowState.INITIAL -> {
            InitialWhiteboardScreen(
                text = uiState.textInput,
                imageUri = uiState.selectedImageUri,
                onTextChange = viewModel::onTextInputChanged,
                onSendClick = {
                    Log.d(TAG, "InitialWhiteboardScreen: Send button clicked.")
                    viewModel.onSendText()
                },
                onImageSelected = viewModel::onImageSelected,
                showFailureMessage = uiState.showConfirmationFailureMessage
            )
        }
        else -> {
            MainWhiteboardContent(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun InitialWhiteboardScreen(
    text: String,
    imageUri: Uri?,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onImageSelected: (Uri?) -> Unit,
    showFailureMessage: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                Log.d(TAG, "Camera result success. URI: $tempCameraImageUri")
                onImageSelected(tempCameraImageUri)
            } else {
                Log.d(TAG, "Camera capture failed or was cancelled.")
            }
        }
    )
    fun launchCamera() {
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        tempCameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    // --- Permission Handling ---
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Camera permission granted.")
                // Permission was granted, launch the camera
                launchCamera()
            } else {
                Log.d(TAG, "Camera permission denied.")
                // Handle permission denial (e.g., show a snackbar or dialog)
            }
        }
    )

    // --- Image Picker Logic ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            Log.d(TAG, "Image picker result: $uri")
            onImageSelected(uri)
        }
    )

    fun launchImageChooser() {
        // We can't directly combine camera into the modern Photo Picker, so we'll just launch it for now.
        // A custom dialog could be used to present both options.
        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showFailureMessage) {
                Text(
                    text = "Por favor, vuelve a intentarlo escribiendo el problema en texto o subiendo una imagen.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }

            if (imageUri != null) {
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    )
                    IconButton(
                        onClick = { onImageSelected(null) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(0.9f),
                placeholder = { Text("Describe tu problema de matemáticas...") },
                maxLines = 10,
                textStyle = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Button to launch image chooser (gallery)
                FloatingActionButton(
                    onClick = { launchImageChooser() },
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier.width(64.dp).height(64.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Select a picture from gallery")
                }
                // Button to launch camera
                FloatingActionButton(
                    onClick = {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                            PackageManager.PERMISSION_GRANTED -> {
                                Log.d(TAG, "Camera permission already granted. Launching camera.")
                                launchCamera()
                            }
                            else -> {
                                Log.d(TAG, "Camera permission not granted. Requesting permission.")
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier.width(64.dp).height(64.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Take a picture")
                }

                val sendEnabled = text.isNotBlank() || imageUri != null
                FloatingActionButton(
                    onClick = onSendClick,
                    containerColor = if (sendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (sendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp).height(64.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send text")
                }
            }
        }
    }
}

@Composable
fun MainWhiteboardContent(
    uiState: WhiteboardState,
    viewModel: WhiteboardViewModel
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        offset += panChange
        Log.d(TAG, "Transform state changed: zoom=$zoomChange, pan=$panChange. New scale=$scale, offset=$offset")
    }

    val animationProgress = remember { Animatable(0f) }
    val tutorMessageAnimatable = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseStrokeWidth by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseStrokeWidth"
    )

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

    // Find the triangle to animate from the state
    val animatedTriangle = uiState.items.filterIsInstance<WhiteboardItem.AnimatedTriangle>().firstOrNull()
    val tutorMessageText = uiState.tutorMessage

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

    // Animate tutor message visibility
    LaunchedEffect(tutorMessageText) {
        if (tutorMessageText != null) {
            tutorMessageAnimatable.snapTo(0f)
            kotlinx.coroutines.delay(2000) // Start animation partway through the main drawing
            tutorMessageAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500)
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
            when (uiState.flowState) {
                WhiteboardFlowState.SOCRATIC_TUTORING -> {
                    WhiteboardBottomBar(
                        text = uiState.textInput,
                        imageUri = uiState.selectedImageUri,
                        onTextChange = viewModel::onTextInputChanged,
                        onSendClick = {
                            Log.d(TAG, "WhiteboardBottomBar: Send button clicked.")
                            viewModel.onSendText()
                        },
                        onImageSelected = viewModel::onImageSelected
                    )
                }
                WhiteboardFlowState.AWAITING_CONFIRMATION, WhiteboardFlowState.INTERPRETING -> {
                    ConfirmationBar(
                        onAccept = {
                            Log.d(TAG, "ConfirmationBar: Accept button clicked.")
                            viewModel.onConfirmationAccept()
                        },
                        onReject = {
                            Log.d(TAG, "ConfirmationBar: Reject button clicked.")
                            viewModel.onConfirmationReject()
                        },
                        isWaiting = uiState.flowState == WhiteboardFlowState.INTERPRETING
                    )
                }
                else -> { /* No bottom bar in other states */ }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .clipToBounds()
        ) {
            // Main drawing canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformState)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                if (animatedTriangle != null) {
                    // --- Figure Calculation Phase ---

                    // 1. Define relative points for the triangle
                    val pA = animatedTriangle.b + animatedTriangle.a
                    val pB = animatedTriangle.b
                    val pC = animatedTriangle.b + animatedTriangle.c

                    // 2. Calculate the bounding box of the triangle figure
                    val figureLeft = listOf(pA.x, pB.x, pC.x).minOrNull() ?: 0f
                    val figureTop = listOf(pA.y, pB.y, pC.y).minOrNull() ?: 0f
                    val figureRight = listOf(pA.x, pB.x, pC.x).maxOrNull() ?: 0f
                    val figureBottom = listOf(pA.y, pB.y, pC.y).maxOrNull() ?: 0f

                    // 3. Calculate the final translation offset to center everything
                    val totalWidth = figureRight - figureLeft
                    val totalHeight = figureBottom - figureTop
                    val canvasCenter = Offset(size.width / 2, size.height / 2)
                    val centeringOffset = canvasCenter - Offset(figureLeft + totalWidth / 2, figureTop + totalHeight / 2)

//                    Log.d(TAG, "Canvas: Centering triangle with offset: $centeringOffset")

                    // --- Drawing Phase ---
                    translate(left = centeringOffset.x, top = centeringOffset.y) {
                        // Animate drawing the triangle sides
                        val progress = animationProgress.value

                        val highlightColor = Color(1f, 0.5f, 0f, pulseAlpha) // Bright Orange

                        // Highlight Sides
                        if (progress >= 3f) { // Only highlight after initial drawing
                            animatedTriangle.highlightedSides.forEach { side ->
                                when (side) {
                                    "AB" -> drawLine(highlightColor, pB, pA, strokeWidth = pulseStrokeWidth, cap = StrokeCap.Round)
                                    "BC" -> drawLine(highlightColor, pB, pC, strokeWidth = pulseStrokeWidth, cap = StrokeCap.Round)
                                    "AC" -> drawLine(highlightColor, pA, pC, strokeWidth = pulseStrokeWidth, cap = StrokeCap.Round)
                                }
                            }
                        }

                        // Highlight Angle
                        if (progress >= 3f && animatedTriangle.highlightedAngle == "B") {
                            val angleSize = 40f
                            val anglePath = Path().apply {
                                moveTo(pB.x + angleSize, pB.y)
                                lineTo(pB.x + angleSize, pB.y - angleSize)
                                lineTo(pB.x, pB.y - angleSize)
                            }
                            drawPath(anglePath, highlightColor, style = Stroke(width = pulseStrokeWidth / 2f))
                        }

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

                        // Draw side length labels
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
                                nativeCanvas.drawText(sideLengths.ab ?: "", midAB.x - 30f, midAB.y, textPaint)
                            }
                            val midBC = pB.lerp(pC, 0.5f)
                            if (progress >= 2f) {
                                nativeCanvas.drawText(sideLengths.bc ?: "", midBC.x, midBC.y + 40f, textPaint)
                            }
                            val midAC = pA.lerp(pC, 0.5f)
                            if (progress >= 3f) {
                                nativeCanvas.drawText(sideLengths.ac ?: "", midAC.x + 25f, midAC.y - 15f, textPaint)
                            }
                        }
                    }
                }
            }

            // --- AI Tutor UI Overlay ---
            if (tutorMessageText != null) {
                TutorOverlay(
                    tutorMessage = tutorMessageText,
                    onChatClick = { /* TODO: Implement chat history */ },
                    visibilityProgress = tutorMessageAnimatable.value,
                    showIcon = uiState.flowState == WhiteboardFlowState.SOCRATIC_TUTORING
                )
            }
        }
    }
}

@Composable
private fun TutorOverlay(
    tutorMessage: String,
    onChatClick: () -> Unit,
    visibilityProgress: Float,
    showIcon: Boolean
) {
    val scale = 0.8f + 0.2f * visibilityProgress
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .graphicsLayer(
                alpha = visibilityProgress,
                scaleX = scale,
                scaleY = scale,
                transformOrigin = TransformOrigin(0f, 0f)
            )
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            if (showIcon) {
                FloatingActionButton(
                    onClick = onChatClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Tutor"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Chat bubble with a triangle pointing up
            Surface(
                modifier = Modifier.width(300.dp),
                shape = ChatBubbleShape(arrowHeight = 8.dp, arrowWidth = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = tutorMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private class ChatBubbleShape(
    private val arrowWidth: Dp,
    private val arrowHeight: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val arrowWidthPx = with(density) { arrowWidth.toPx() }
        val arrowHeightPx = with(density) { arrowHeight.toPx() }
        val cornerRadius = with(density) { 16.dp.toPx() }

        val path = Path().apply {
            // Start from top-left, after the arrow
            moveTo(x = 0f, y = arrowHeightPx)

            // Top-left corner
            arcTo(
                rect = Rect(left = 0f, top = arrowHeightPx, right = cornerRadius, bottom = arrowHeightPx + cornerRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Top edge
            lineTo(x = size.width - cornerRadius, y = arrowHeightPx)

            // Top-right corner
            arcTo(
                rect = Rect(left = size.width - cornerRadius, top = arrowHeightPx, right = size.width, bottom = arrowHeightPx + cornerRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Right edge
            lineTo(x = size.width, y = size.height - cornerRadius)

            // Bottom-right corner
            arcTo(
                rect = Rect(left = size.width - cornerRadius, top = size.height - cornerRadius, right = size.width, bottom = size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Bottom edge
            lineTo(x = cornerRadius, y = size.height)

            // Bottom-left corner
            arcTo(
                rect = Rect(left = 0f, top = size.height - cornerRadius, right = cornerRadius, bottom = size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Left edge
            lineTo(x = 0f, y = arrowHeightPx + cornerRadius)


            // Arrow
            val arrowStartX = with(density) { 24.dp.toPx() }
            moveTo(x = arrowStartX, y = arrowHeightPx)
            lineTo(x = arrowStartX + (arrowWidthPx / 2), y = 0f) // Point up
            lineTo(x = arrowStartX + arrowWidthPx, y = arrowHeightPx)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun ConfirmationBar(
    onAccept: () -> Unit,
    onReject: () -> Unit,
    isWaiting: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onReject,
                enabled = !isWaiting,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.width(80.dp).height(80.dp)
            ) {
                Icon(Icons.Default.Close, "Reject Interpretation", modifier = Modifier.fillMaxSize(0.6f))
            }
            Spacer(modifier = Modifier.width(32.dp))
            IconButton(
                onClick = onAccept,
                enabled = !isWaiting,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.width(80.dp).height(80.dp)
            ) {
                Icon(Icons.Default.Check, "Accept Interpretation", modifier = Modifier.fillMaxSize(0.6f))
            }
        }
    }
}

@Composable
private fun WhiteboardBottomBar(
    text: String,
    imageUri: Uri?,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onImageSelected: (Uri?) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = onImageSelected
    )

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (imageUri != null) {
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // Allow re-picking image
                                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    )
                    IconButton(
                        onClick = { onImageSelected(null) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                         colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask a question or add an image...") },
                    maxLines = 5
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
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
                Spacer(modifier = Modifier.width(8.dp))

                val sendEnabled = text.isNotBlank() || imageUri != null
                FloatingActionButton(
                    onClick = onSendClick,
                    containerColor = if (sendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (sendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}
