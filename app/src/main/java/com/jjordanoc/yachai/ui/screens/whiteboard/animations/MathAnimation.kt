package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.runtime.Composable

/**
 * Common interface for all math animations that can be displayed on the whiteboard.
 * Each animation type implements this interface to provide its rendering logic.
 */
interface MathAnimation {
    /**
     * Unique identifier for this animation instance
     */
    val id: String
    
    /**
     * Render this animation as a Composable
     */
    @Composable
    fun draw()
} 