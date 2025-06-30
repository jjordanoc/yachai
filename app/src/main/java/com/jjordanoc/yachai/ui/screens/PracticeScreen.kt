package com.jjordanoc.yachai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun PracticeScreen(
    navController: NavController,
    practiceViewModel: PracticeViewModel = viewModel()
) {
    val uiState by practiceViewModel.uiState.collectAsState()
    var answer by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Problema de Práctica",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (val state = uiState) {
            is PracticeUiState.Loading -> {
                CircularProgressIndicator()
            }
            is PracticeUiState.Problem -> {
                Text(text = state.text, style = MaterialTheme.typography.bodyLarge)
                feedbackText = ""
            }
            is PracticeUiState.Feedback -> {
                feedbackText = state.message
            }
            is PracticeUiState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("Escribe tu respuesta aquí") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { practiceViewModel.checkAnswer(answer) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState is PracticeUiState.Problem
        ) {
            Text("Revisar Respuesta")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (feedbackText.isNotEmpty()) {
            Text(
                text = feedbackText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (feedbackText.startsWith("¡Correcto!")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
} 