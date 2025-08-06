package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import com.jjordanoc.yachai.ui.screens.whiteboard.model.AnimationCommand
import com.jjordanoc.yachai.ui.theme.*

/**
 * Animation for displaying mathematical expressions in a readable, prominent font.
 * The expression is displayed in a card-like container with proper styling.
 */
class ExpressionAnimation(
    val expression: String,
    override val id: String = UUID.randomUUID().toString()
) : MathAnimation {

    // Educational color hierarchy for whiteboard
    
    @Composable
    override fun draw() {
        Box(
            modifier = Modifier
                .background(
                    color = baseWhite.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = expression,
                color = baseWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Left
            )
        }
    }
    
    companion object {
        /**
         * Static signature for this animation type
         */
        val signature = AnimationSignature(
            command = "drawExpression",
            description = "Escribe una expresión matemática en la pizarra",
            args = mapOf("expression" to "Texto de la expresión")
        )
        
        /**
         * Create an ExpressionAnimation from a command
         */
        fun fromCommand(command: AnimationCommand): ExpressionAnimation? {
            val expression = command.args.expression
            
            return if (expression != null && expression.isNotBlank()) {
                ExpressionAnimation(expression = expression)
            } else {
                null
            }
        }
    }
} 