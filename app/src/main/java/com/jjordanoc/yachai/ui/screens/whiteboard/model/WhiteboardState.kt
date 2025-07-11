package com.jjordanoc.yachai.ui.screens.whiteboard.model

import android.net.Uri
import com.jjordanoc.yachai.ui.screens.whiteboard.WhiteboardFlowState

data class WhiteboardState(
    val gridItems: Map<Pair<Int, Int>, WhiteboardItem> = emptyMap(),
    val textInput: String = "",
    val tutorMessage: String? = "Hello! I'm your AI Math Tutor. Describe a problem to me, and we can solve it together.",
    val hint: String? = null,
    val flowState: WhiteboardFlowState = WhiteboardFlowState.INITIAL,
    val initialProblemStatement: String = "",
    val showConfirmationFailureMessage: Boolean = false,
    val isModelLoading: Boolean = true,
    val selectedImageUri: Uri? = null,
)