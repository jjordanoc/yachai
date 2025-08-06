package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

/**
 * Auto-filling whiteboard that displays animations in a responsive grid layout.
 * Uses FlowRow to automatically wrap animations to new rows when they don't fit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhiteboardAutoFill(animations: List<MathAnimation>) {
    FlowRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = Int.MAX_VALUE // No limit, auto-wrap
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