package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size

/**
 * Composable that renders a list of math animations on a canvas.
 * Each animation in the list is drawn in order.
 */
@Composable
fun WhiteboardCanvas(
    animations: List<MathAnimation>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        animations.forEach { animation ->
            if (animation.isVisible()) {
                animation.draw(this)
            }
        }
    }
} 