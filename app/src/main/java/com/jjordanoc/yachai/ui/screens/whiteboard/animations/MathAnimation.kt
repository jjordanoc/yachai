package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Common interface for all math animations that can be drawn on the whiteboard.
 * Each animation type implements this interface to provide its drawing logic.
 */
interface MathAnimation {
    /**
     * Unique identifier for this animation instance
     */
    val id: String
    
    /**
     * Draw this animation on the canvas
     * @param drawScope The DrawScope to draw on
     */
    fun draw(drawScope: DrawScope)
    
    /**
     * Check if this animation should be visible
     */
    fun isVisible(): Boolean = true
} 