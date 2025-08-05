package com.jjordanoc.yachai.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jjordanoc.yachai.ui.screens.*
import com.jjordanoc.yachai.ui.screens.whiteboard.HorizontalTutorialScreen
import com.jjordanoc.yachai.ui.screens.ModelLoadingScreen
import com.jjordanoc.yachai.ui.screens.whiteboard.ProblemInputScreen
import com.jjordanoc.yachai.ui.screens.whiteboard.ProblemLoadingScreen
import com.jjordanoc.yachai.ui.screens.whiteboard.TutorialViewModel
import com.jjordanoc.yachai.ui.screens.whiteboard.TutorialViewModelFactory

object Routes {
    const val SPLASH_SCREEN = "splash"
    const val ONBOARDING_SCREEN = "onboarding"
    const val MAIN_SCREEN = "main"
    const val CAMERA_SCREEN = "camera"
    const val RESULT_SCREEN = "result/{imageUri}"
    const val PRACTICE_SCREEN = "practice"
    const val WHITEBOARD_SCREEN = "whiteboard"
    const val CHAT_SCREEN = "chat"
    const val HORIZONTAL_TUTORIAL_SCREEN = "horizontal_tutorial"
    const val MODEL_LOADING_SCREEN = "model_loading"
    const val PROBLEM_INPUT_SCREEN = "problem_input"
    const val PROBLEM_LOADING_SCREEN = "problem_loading"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    // Create shared ViewModel for the input -> tutorial flow
    val context = LocalContext.current
    val sharedTutorialViewModel: TutorialViewModel = viewModel(
        factory = TutorialViewModelFactory(
            context.applicationContext as Application
        )
    )
    
    NavHost(navController = navController, startDestination = Routes.SPLASH_SCREEN) {
        composable(Routes.SPLASH_SCREEN) {
            SplashScreen(navController = navController)
        }
        composable(Routes.ONBOARDING_SCREEN) {
            OnboardingScreen(navController = navController)
        }
        composable(Routes.MODEL_LOADING_SCREEN) {
            ModelLoadingScreen(
                navController = navController,
                viewModel = sharedTutorialViewModel
            )
        }
        composable(Routes.MAIN_SCREEN) {
            MainScreen(navController = navController)
        }

        composable(Routes.HORIZONTAL_TUTORIAL_SCREEN) {
            HorizontalTutorialScreen(
                navController = navController,
                viewModel = sharedTutorialViewModel
            )
        }
        
        composable(Routes.PROBLEM_INPUT_SCREEN) {
            ProblemInputScreen(
                navController = navController,
                viewModel = sharedTutorialViewModel
            )
        }
        
        composable(Routes.PROBLEM_LOADING_SCREEN) {
            ProblemLoadingScreen(
                navController = navController,
                viewModel = sharedTutorialViewModel
            )
        }
    }
}