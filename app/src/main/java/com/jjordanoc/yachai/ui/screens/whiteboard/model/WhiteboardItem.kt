package com.jjordanoc.yachai.ui.screens.whiteboard.model

import androidx.compose.ui.geometry.Offset

enum class RectanglePhase {
    SETUP,           // Show empty rectangle outline
    VERTICAL_LINES,  // Show vertical grid lines first
    FILLING_ROWS     // Fill unit squares row by row
}

enum class GridPhase {
    SETUP,           // Show base rectangle
    GRID_LINES,      // Show grid lines 
    FILLING_UNITS    // Fill unit squares with smooth animation
}

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

    data class AnimatedRectangle(
        val length: Int,          // horizontal dimension
        val width: Int,           // vertical dimension  
        val currentColumn: Int = 0,    // Current column being filled (0 to length-1)
        val currentRow: Int = 0,       // Current row being filled (0 to width-1)
        val animationPhase: RectanglePhase = RectanglePhase.SETUP,
        val showDimensions: Boolean = true,
        val lengthLabel: String = "longitud",
        val widthLabel: String = "ancho"
    ) : WhiteboardItem()

    data class AnimatedGrid(
        val length: Int,          // horizontal dimension (number of unit squares)
        val width: Int,           // vertical dimension (number of unit squares)
        val unit: String = "1",   // unit label (e.g., "1m²", "1cm²")
        val animationPhase: GridPhase = GridPhase.SETUP,
        val currentColumn: Int = 0,    // Current column being filled (0 to length-1)
        val currentRow: Int = 0,       // Current row being filled (0 to width-1)
        val fillProgress: Float = 0f,  // Progress of current unit square filling (0f to 1f)
        val showDimensions: Boolean = true,
        val lengthLabel: String = "largo",
        val widthLabel: String = "ancho"
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