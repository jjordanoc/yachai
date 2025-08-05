package com.jjordanoc.yachai.ui.screens.whiteboard

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.Routes
import com.jjordanoc.yachai.ui.theme.TutorialTeal
import com.jjordanoc.yachai.ui.theme.White
import com.jjordanoc.yachai.utils.TAG

@Composable
fun ProblemInputScreen(
    navController: NavController,
    viewModel: TutorialViewModel = viewModel(
        factory = TutorialViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
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
    
    // --- Speech Recognition Setup ---
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    
    // Function to start speech recognition
    fun startSpeechRecognition() {
        Log.d(TAG, "Attempting to start speech recognition")
        Log.d(TAG, "Speech recognizer null: ${speechRecognizer == null}")
        Log.d(TAG, "Recognition available: ${SpeechRecognizer.isRecognitionAvailable(context)}")
        
        speechRecognizer?.let { recognizer ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES") // Spanish language
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe tu problema de matemáticas...")
                // Enable offline recognition if available
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            recognizer.startListening(intent)
            Log.d(TAG, "Started speech recognition")
        } ?: run {
            Log.w(TAG, "Speech recognizer is null when trying to start recognition")
            
            // Try to reinitialize if recognition is now available
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.d(TAG, "Recognition now available, trying to reinitialize...")
                Toast.makeText(context, "Inicializando reconocimiento de voz...", Toast.LENGTH_SHORT).show()
                // We can't reinitialize here easily due to scope, so just inform user
            } else {
                Toast.makeText(context, "Reconocimiento de voz no disponible. Verifica que Google Speech Services esté instalado.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                startSpeechRecognition()
            } else {
                Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // --- Speech Recognition Setup ---
    DisposableEffect(context) {
        val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(TAG, "Speech recognition available: $isAvailable")
        
        if (isAvailable) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "Speech recognition ready")
                        isListening = true
                    }
                    
                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "Speech recognition started")
                    }
                    
                    override fun onRmsChanged(rmsdB: Float) {
                        // Voice level feedback - could be used for visual feedback
                    }
                    
                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Audio buffer - not needed for basic implementation
                    }
                    
                    override fun onEndOfSpeech() {
                        Log.d(TAG, "Speech recognition ended")
                        isListening = false
                    }
                    
                    override fun onError(error: Int) {
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                            else -> "Unknown error: $error"
                        }
                        Log.e(TAG, "Speech recognition error: $errorMessage")
                        isListening = false
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            Toast.makeText(context, "Error de reconocimiento: $errorMessage", Toast.LENGTH_LONG).show()
                        } else if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                            Toast.makeText(context, "No se pudo entender lo que dijiste. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    override fun onResults(results: Bundle?) {
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                            if (matches.isNotEmpty()) {
                                val recognizedText = matches[0]
                                Log.d(TAG, "Speech recognized: $recognizedText")
                                viewModel.onTextInputChanged(recognizedText)
                                isListening = false
                            }
                        }
                    }
                    
                    override fun onPartialResults(partialResults: Bundle?) {
                        // Could be used for real-time text updates
                    }
                    
                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Additional events - not needed for basic implementation
                    }
                })
            }
            Log.d(TAG, "Speech recognizer initialized successfully")
        } else {
            Log.w(TAG, "Speech recognition not available on this device")
            
            // Try to provide more specific guidance
            val packageManager = context.packageManager
            val hasGoogleApp = try {
                packageManager.getPackageInfo("com.google.android.googlequicksearchbox", 0)
                true
            } catch (e: Exception) {
                false
            }
            
            val message = if (!hasGoogleApp) {
                "Reconocimiento de voz no disponible. Instala la app de Google desde Play Store."
            } else {
                "Reconocimiento de voz no disponible. Verifica que Google Speech Services esté habilitado en Configuración."
            }
            
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            Log.w(TAG, "Google app available: $hasGoogleApp")
        }
        
        onDispose {
            Log.d(TAG, "Cleaning up speech recognizer")
            speechRecognizer?.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Alpaca image at the top
        Box(
            modifier = Modifier
                .size(width = 240.dp, height = 160.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.alpakey),
                contentDescription = "Alpaca tutor",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(15.dp))
        
        // Top bar with settings gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Greeting message
            Text(
                text = "¡Hola! ¿Qué problema quieres resolver hoy?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.weight(1f)
            )
            
            // Settings gear for GPU toggle
            IconButton(
                onClick = { 
                    // TODO: Open settings dialog for GPU toggle
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        

        Spacer(modifier = Modifier.height(15.dp))
        
        // Input area with buttons - horizontal layout
        Row() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE), shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text input field (takes most space)
                TextField(
                    value = uiState.textInput,
                    onValueChange = { viewModel.onTextInputChanged(it) },
                    label = { Text("Escribe tu problema de matemáticas") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor  = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,   // remove underline/border
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor  = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !uiState.isProcessing
                )

                // Right side buttons column
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image upload button with badge
                    Box {
                        IconButton(
                            onClick = {
                                when (android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) -> {
                                        launchImageCropper()
                                    }
                                    else -> {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333333))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Upload image",
                                tint = White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Badge showing image count
                        if (uiState.selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(TutorialTeal)
                            ) {
                                Text(
                                    text = "1",
                                    color = White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    // Microphone button
                    IconButton(
                        onClick = {
                            when (android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) -> {
                                    startSpeechRecognition()
                                }
                                else -> {
                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isListening) TutorialTeal else Color(0xFF333333))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                    }


                }
            // Send button
            IconButton(
                onClick = {
                    if (uiState.textInput.isNotBlank() || uiState.selectedImageUri != null) {
                        viewModel.onSendText()
                        // Navigate to tutorial screen after sending
                        navController.navigate(Routes.HORIZONTAL_TUTORIAL_SCREEN)
                    }
                },
                enabled = !uiState.isProcessing && (uiState.textInput.isNotBlank() || uiState.selectedImageUri != null),
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isProcessing) Color.Gray else TutorialTeal)
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        }
    }
}