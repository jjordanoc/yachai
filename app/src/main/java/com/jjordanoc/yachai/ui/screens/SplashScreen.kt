package com.jjordanoc.yachai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jjordanoc.yachai.data.Models
import com.jjordanoc.yachai.data.isDownloaded
import com.jjordanoc.yachai.ui.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val model = Models.GEMMA_3N_E2B_VISION

    LaunchedEffect(Unit) {
        delay(1000) // Simulate a loading time
        val destination = if (model.isDownloaded(context)) {
            Routes.MAIN_SCREEN
        } else {
            Routes.ONBOARDING_SCREEN
        }
        navController.navigate(destination) {
            popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
} 