package com.jjordanoc.yachai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.theme.TutorialGreen
import com.jjordanoc.yachai.ui.theme.TutorialTeal
import com.jjordanoc.yachai.ui.theme.TutorialGray
import com.jjordanoc.yachai.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun HorizontalTutorialScreen(navController: NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    var inputText by remember { mutableStateOf("") }
    var isAlpacaSpeaking by remember { mutableStateOf(false) }
    
    // Animation for alpaca speaking
    val speakingAnimation = rememberInfiniteTransition(label = "alpaca_speaking")
    val isMouthOpen by speakingAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_animation"
    )
    
    // Function to trigger alpaca speaking
    fun triggerAlpacaSpeaking(duration: Long = 3000L) {
        isAlpacaSpeaking = true
    }
    
    // Stop speaking after duration
    LaunchedEffect(isAlpacaSpeaking) {
        if (isAlpacaSpeaking) {
            delay(3000) // Speak for 3 seconds
            isAlpacaSpeaking = false
        }
    }
    
    // Trigger speaking animation when lesson starts
    LaunchedEffect(Unit) {
        delay(1000) // Wait 1 second after screen loads
        triggerAlpacaSpeaking()
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
                        triggerAlpacaSpeaking()
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
                
                // Main content text - positioned in the left side to leave room for alpaca
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 80.dp, end = if (isLandscape) 200.dp else 80.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
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
                
                // Right navigation button
                IconButton(
                    onClick = { 
                        // Handle right navigation and trigger alpaca speaking
                        triggerAlpacaSpeaking()
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
                // Determine which image to show based on speaking state
                val alpacaImage = if (isAlpacaSpeaking && isMouthOpen > 0.5f) {
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
        
        // Chat interface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
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
                        text = "Escribe tu pregunta aquí...",
                        color = Color.Gray
                    )
                }
            )
            
            // Send button
            FloatingActionButton(
                onClick = { 
                    if (inputText.isNotBlank()) {
                        // Handle send action
                        println("Sending: $inputText")
                        // Clear input and trigger alpaca response
                        inputText = ""
                        triggerAlpacaSpeaking()
                    }
                },
                modifier = Modifier.size(70.dp),
                shape = CircleShape,
                containerColor = TutorialTeal,
                contentColor = White
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
                    // Handle microphone action
                    triggerAlpacaSpeaking() 
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