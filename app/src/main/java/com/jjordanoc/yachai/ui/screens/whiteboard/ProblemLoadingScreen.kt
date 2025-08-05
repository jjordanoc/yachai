package com.jjordanoc.yachai.ui.screens.whiteboard

import android.app.Application
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.Routes
import com.jjordanoc.yachai.ui.theme.White

@Composable
fun ProblemLoadingScreen(
    navController: NavController,
    viewModel: TutorialViewModel = viewModel(
        factory = TutorialViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Debug logging for image URI and processing state
    LaunchedEffect(uiState.selectedImageUri, uiState.isProcessing) {
        Log.d("ProblemLoadingScreen", "Selected image URI: ${uiState.selectedImageUri}")
        Log.d("ProblemLoadingScreen", "Text input: ${uiState.textInput}")
        Log.d("ProblemLoadingScreen", "Is processing: ${uiState.isProcessing}")
    }
    
    // Navigation logic - navigate to tutorial screen when processing finishes and we have a tutor message
    LaunchedEffect(uiState.isProcessing, uiState.tutorMessage) {
        if (!uiState.isProcessing) {
            if (uiState.tutorMessage != null) {
                // Success - navigate to tutorial
                navController.navigate(Routes.HORIZONTAL_TUTORIAL_SCREEN) {
                    // Clear the back stack so user can't go back to loading screen
                    popUpTo(Routes.PROBLEM_LOADING_SCREEN) { inclusive = true }
                }
            } else {
                // Error or no response - go back to problem input
                navController.popBackStack()
            }
        }
    }
    
    // Animated dots for loading
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(30.dp)
    ) {
        // Cancel button in top-right
        Button(
            onClick = {
                // Cancel LLM inference and navigate back to problem input
                viewModel.cancelLlmInference()
                navController.navigate(Routes.PROBLEM_INPUT_SCREEN) {
                    // Clear the back stack so user goes directly to problem input
                    popUpTo(Routes.PROBLEM_INPUT_SCREEN) { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF7B7B)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 15.dp)
        ) {
            Text(
                text = "Cancelar",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Main content centered
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp), // Space for cancel button
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Alpaca and thinking message
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alpaca image
                Image(
                    painter = painterResource(id = R.drawable.alpakey),
                    contentDescription = "Alpaca tutor thinking",
                    modifier = Modifier
                        .width(200.dp)
                        .height(133.dp),
                    contentScale = ContentScale.Fit
                )
                
                // Thinking message with animated dots
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Text(
                            text = "Estoy pensando",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        
                        // Animated dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = dot1Alpha),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = dot2Alpha),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = dot3Alpha),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    
                    Text(
                        text = "Esto puede tomar de ~2-3 minutos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Problem display section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 15.dp)
            ) {
                Text(
                    text = "Tu problema:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                // Conditional layout based on content
                when {
                    // Both image and text
                    uiState.selectedImageUri != null && uiState.textInput.isNotBlank() -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Image section (half width)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .background(
                                        color = Color(0xFFD9D9D9),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = 3.dp,
                                        color = Color.Black,
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                var imageLoadError by remember { mutableStateOf(false) }
                                
                                if (!imageLoadError) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = uiState.selectedImageUri,
                                            onError = {
                                                Log.e("ProblemLoadingScreen", "Failed to load image: ${uiState.selectedImageUri}")
                                                imageLoadError = true
                                            },
                                            onSuccess = {
                                                Log.d("ProblemLoadingScreen", "Image loaded successfully: ${uiState.selectedImageUri}")
                                            }
                                        ),
                                        contentDescription = "Problem image",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        text = "Error cargando imagen",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Red,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            
                            // Text section (half width)
                            Text(
                                text = "\"${uiState.textInput}\"",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Only image
                    uiState.selectedImageUri != null -> {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(120.dp)
                                .background(
                                    color = Color(0xFFD9D9D9),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 3.dp,
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            var imageLoadError by remember { mutableStateOf(false) }
                            
                            if (!imageLoadError) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = uiState.selectedImageUri,
                                        onError = {
                                            Log.e("ProblemLoadingScreen", "Failed to load image: ${uiState.selectedImageUri}")
                                            imageLoadError = true
                                        },
                                        onSuccess = {
                                            Log.d("ProblemLoadingScreen", "Image loaded successfully: ${uiState.selectedImageUri}")
                                        }
                                    ),
                                    contentDescription = "Problem image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    text = "Error cargando imagen",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Red,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    // Only text
                    uiState.textInput.isNotBlank() -> {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(120.dp)
                                .background(
                                    color = Color(0xFFD9D9D9),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 3.dp,
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\"${uiState.textInput}\"",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    
                    // Neither image nor text (shouldn't happen in normal flow)
                    else -> {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(120.dp)
                                .background(
                                    color = Color(0xFFD9D9D9),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 3.dp,
                                    color = Color.Black,
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin contenido",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}