package com.jjordanoc.yachai.ui.screens

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jjordanoc.yachai.ui.Routes
import com.jjordanoc.yachai.ui.screens.whiteboard.TutorialViewModel
import com.jjordanoc.yachai.ui.screens.whiteboard.TutorialViewModelFactory

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