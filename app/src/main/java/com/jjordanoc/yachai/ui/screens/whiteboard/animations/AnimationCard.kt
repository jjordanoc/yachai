package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Card container for individual animations.
 * Provides consistent styling and padding for all animation elements.
 * Includes a smooth fade-in with scale animation when appearing.
 */
@Composable
fun AnimationCard(
    animation: MathAnimation,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 600, easing = EaseOutCubic)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(durationMillis = 600, easing = EaseOutCubic)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp)
        ) {
            animation.draw()
        }
    }
} 