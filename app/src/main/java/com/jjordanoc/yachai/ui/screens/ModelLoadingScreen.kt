package com.jjordanoc.yachai.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jjordanoc.yachai.R
import com.jjordanoc.yachai.ui.Routes
import com.jjordanoc.yachai.ui.screens.whiteboard.TutorialViewModel
import com.jjordanoc.yachai.ui.screens.whiteboard.TutorialViewModelFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ModelLoadingScreen(
    navController: NavController,
    viewModel: TutorialViewModel = viewModel(
        factory = TutorialViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to ProblemInputScreen when model is loaded
    LaunchedEffect(uiState.isModelLoading) {
        if (!uiState.isModelLoading) {
            navController.navigate(Routes.PROBLEM_INPUT_SCREEN) {
                popUpTo(Routes.MODEL_LOADING_SCREEN) { inclusive = true }
            }
        }
    }

    // Animated messages for "Usamos IA avanzada"
    val messages = listOf(
        "🤖 Usamos IA avanzada que funciona sin internet",
        "⏱ Esto toma tiempo pero vale la pena",
        "🎯 Recibirás explicaciones de problemas paso a paso"
    )

    var currentMessageIndex by remember { mutableStateOf(0) }
    var currentMessage by remember { mutableStateOf(messages[0]) }

    // Animate messages every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // 5 seconds
            currentMessageIndex = (currentMessageIndex + 1) % messages.size
            currentMessage = messages[currentMessageIndex]
        }
    }

    // Progress animation for 30 seconds
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 500,
            easing = LinearEasing
        ),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        val duration = 30000L // 30 seconds
        val steps = 100
        val stepDuration = duration / steps
        for (i in 1..steps) {
            progress = i / steps.toFloat()
            delay(stepDuration)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 30.dp, vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Alpaca and greeting section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alpaca image
            Image(
                painter = painterResource(id = R.drawable.alpakey),
                contentDescription = "Alpaca tutor",
                modifier = Modifier.size(width = 200.dp, height = 134.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Greeting text
            Text(
                text = "Hola, soy Paka, tu amigo matemático.",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Left
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Progress section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Animated message
            AnimatedContent(
                targetState = currentMessage,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                }
            ) { message ->
                Text(
                    text = message,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }

            // Progress bar and percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Progress bar
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .height(25.dp)
                        .background(
                            color = Color(0xFFCFCFCF),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFBDBDBD),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    // Progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width((400.dp * animatedProgress))
                            .background(
                                color = Color(0xFF4DB6AC), // TutorialTeal
                                shape = RoundedCornerShape(20.dp)
                            )
                    )
                }

                // Percentage text
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            // Estimated time
            Text(
                text = "Tiempo estimado: ~30 segundos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}
