package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.runtime.Composable
import com.jjordanoc.yachai.ui.screens.whiteboard.model.AnimationCommand

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
    
    companion object {
        /**
         * Create an animation from a command, or null if the command is not supported
         */
        fun fromCommand(command: AnimationCommand): MathAnimation? {
            return when (command.command) {
                "drawRectangle" -> RectangleAnimation.fromCommand(command)
                "drawExpression" -> ExpressionAnimation.fromCommand(command)
                else -> null
            }
        }
        
        /**
         * Get all available animation signatures for LLM prompts
         */
        fun getAllSignatures(): List<AnimationSignature> {
            return listOf(
                RectangleAnimation.signature,
                ExpressionAnimation.signature
            )
        }
    }
} 