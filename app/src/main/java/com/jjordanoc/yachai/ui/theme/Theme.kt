package com.jjordanoc.yachai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val YachAIColorScheme = lightColorScheme(
    primary = TutorialTeal,
    secondary = secondaryTeal,
    tertiary = focusAmber,
    background = baseWhite,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = cancelRed,
    onError = White
)

@Composable
fun YachAITheme(
    darkTheme: Boolean = false, // Force light theme for YachAI
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain brand consistency
    content: @Composable () -> Unit
) {
    val colorScheme = YachAIColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TutorialGreen.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}