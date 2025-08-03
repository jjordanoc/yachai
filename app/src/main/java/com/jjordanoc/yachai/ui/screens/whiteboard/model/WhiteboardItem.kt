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
    
    // Data visualization items
    data class DataTable(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : WhiteboardItem()
    
    data class TallyChart(
        val categories: List<String>,
        val counts: List<Int>
    ) : WhiteboardItem()
    
    data class BarChart(
        val labels: List<String>,
        val values: List<Int>,
        val highlightedIndex: Int? = null
    ) : WhiteboardItem()
    
    data class PieChart(
        val labels: List<String>,
        val values: List<Int>,
        val highlightedIndex: Int? = null
    ) : WhiteboardItem()
    
    data class DotPlot(
        val values: List<Int>,
        val min: Int,
        val max: Int,
        val highlightedIndices: List<Int> = emptyList()
    ) : WhiteboardItem()
    
    data class DataSummary(
        val summary: String,
        val meanValue: Double? = null,
        val rangeMin: Int? = null,
        val rangeMax: Int? = null
    ) : WhiteboardItem()
}