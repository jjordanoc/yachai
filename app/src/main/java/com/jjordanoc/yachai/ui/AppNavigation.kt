package com.jjordanoc.yachai.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jjordanoc.yachai.ui.screens.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
        composable(Routes.RESULT_SCREEN) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            val decodedUri = URLDecoder.decode(imageUri, StandardCharsets.UTF_8.toString())
            ResultScreen(navController = navController, imageUri = decodedUri)
        }
        composable(Routes.PRACTICE_SCREEN) {
            PracticeScreen(navController = navController)
        }
        composable(Routes.WHITEBOARD_SCREEN) {
            WhiteboardScreen()
        }
        composable(Routes.CHAT_SCREEN) {
            ChatScreen()
        }
    }
}