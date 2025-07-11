package com.jjordanoc.yachai.ui.screens.whiteboard.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.jjordanoc.yachai.ui.screens.whiteboard.SideLengths

sealed class WhiteboardItem {
    data class DrawingPath(val path: Path, val color: Color, val strokeWidth: Float) : WhiteboardItem()
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
}