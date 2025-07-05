package com.jjordanoc.yachai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jjordanoc.yachai.data.Models
import com.jjordanoc.yachai.ui.Routes

@Composable
fun OnboardingScreen(
    navController: NavController,
    onboardingViewModel: OnboardingViewModel = viewModel()
) {
    val downloadState by onboardingViewModel.downloadState.collectAsState()

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Success) {
            navController.navigate(Routes.MAIN_SCREEN) {
                popUpTo(Routes.ONBOARDING_SCREEN) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "¡Bienvenido a YachAI!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Para comenzar, necesitamos descargar el motor de inteligencia artificial. Esto solo tomará un momento y solo se hará una vez.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (val state = downloadState) {
            is DownloadState.Idle -> {
                Button(onClick = { onboardingViewModel.downloadModel(Models.GEMMA_3N_E2B_VISION) }) {
                    Text("Descargar YachAI")
                }
            }
            is DownloadState.Downloading -> {
                val progress = if (state.progress.totalBytes > 0) {
                    state.progress.receivedBytes.toFloat() / state.progress.totalBytes.toFloat()
                } else {
                    0f
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Descargando... ${state.progress.receivedBytes} / ${state.progress.totalBytes}")
                }
            }
            is DownloadState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onboardingViewModel.downloadModel(Models.GEMMA_3N_E2B_VISION) }) {
                    Text("Intentar de Nuevo")
                }
            }
            is DownloadState.Success -> {
                // Handled in LaunchedEffect
            }
        }
    }
} 