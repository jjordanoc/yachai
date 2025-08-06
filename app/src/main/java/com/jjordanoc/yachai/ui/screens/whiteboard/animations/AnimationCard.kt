package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Card container for individual animations.
 * Provides consistent styling and padding for all animation elements.
 */
@Composable
fun AnimationCard(
    animation: MathAnimation,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        animation.draw()
    }
} 