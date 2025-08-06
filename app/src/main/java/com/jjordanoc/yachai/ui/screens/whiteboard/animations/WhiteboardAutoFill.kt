package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn

/**
 * Auto-filling whiteboard that displays animations in a responsive grid layout.
 * Uses FlowColumn with fixed height and horizontal scroll when content overflows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhiteboardAutoFill(animations: List<MathAnimation>) {
    val horizontalScrollState = rememberScrollState()
    
    FlowColumn(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachColumn = Int.MAX_VALUE // No limit, let it wrap naturally
    ) {
        animations.forEach { animation ->
            AnimationCard(
                animation = animation,
                modifier = Modifier
                    .padding(5.dp) // Small spacing between cards
            )
        }
    }
} 