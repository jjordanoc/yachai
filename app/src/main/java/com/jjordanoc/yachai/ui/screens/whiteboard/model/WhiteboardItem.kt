package com.jjordanoc.yachai.ui.screens.whiteboard.model

import androidx.compose.ui.geometry.Offset

sealed class WhiteboardItem {
    data class AnimatedTriangle(
        val a: Offset,
        val b: Offset,
        val c: Offset,
        val sideLengths: SideLengths,
        val highlightedSides: List<String> = emptyList(),
        val highlightedAngle: String? = null
    ) : WhiteboardItem()

    data class AnimatedNumberLine(
        val range: List<Int>,
        val marks: List<Int>,
        val highlight: List<Int>
    ) : WhiteboardItem()

    data class Expression(val text: String) : WhiteboardItem()
}