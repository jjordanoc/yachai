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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.jjordanoc.yachai.ui.theme.White

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
    
    // Navigation logic - navigate to loading screen when processing starts
    LaunchedEffect(uiState.isProcessing) {
        if (uiState.isProcessing) {
            navController.navigate(Routes.PROBLEM_LOADING_SCREEN)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Alpaca image
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

            // Greeting message
            Text(
                text = "¡Hola! ¿Qué problema quieres resolver hoy?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(15.dp))

            // Input area using the reusable component
            ProblemInputBar(
                textInput = uiState.textInput,
                onTextInputChanged = { viewModel.onTextInputChanged(it) },
                onSendText = { viewModel.onSendText() },
                onImageSelected = { viewModel.onImageSelected(it) },
                selectedImageUri = uiState.selectedImageUri,
                isProcessing = uiState.isProcessing
            )
        }

        // Settings gear floating in top right
        IconButton(
            onClick = {
                // TODO: Open settings dialog for GPU toggle
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 30.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}