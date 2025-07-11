package com.jjordanoc.yachai.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jjordanoc.yachai.ui.screens.*
import com.jjordanoc.yachai.ui.screens.whiteboard.WhiteboardScreen

object Routes {
    const val SPLASH_SCREEN = "splash"
    const val ONBOARDING_SCREEN = "onboarding"
    const val MAIN_SCREEN = "main"
    const val CAMERA_SCREEN = "camera"
    const val RESULT_SCREEN = "result/{imageUri}"
    const val PRACTICE_SCREEN = "practice"
    const val WHITEBOARD_SCREEN = "whiteboard"
    const val CHAT_SCREEN = "chat"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SPLASH_SCREEN) {
        composable(Routes.SPLASH_SCREEN) {
            SplashScreen(navController = navController)
        }
        composable(Routes.ONBOARDING_SCREEN) {
            OnboardingScreen(navController = navController)
        }
        composable(Routes.MAIN_SCREEN) {
            MainScreen(navController = navController)
        }
        composable(Routes.CAMERA_SCREEN) {
            CameraScreen(navController = navController)
        }
        composable(Routes.WHITEBOARD_SCREEN) {
            WhiteboardScreen()
        }
        composable(Routes.CHAT_SCREEN) {
            ChatScreen()
        }
    }
}