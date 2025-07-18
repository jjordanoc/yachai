package com.jjordanoc.yachai.ui.screens.whiteboard.model

import android.net.Uri
import com.jjordanoc.yachai.ui.screens.whiteboard.WhiteboardFlowState

data class WhiteboardState(
    val gridItems: Map<Pair<Int, Int>, WhiteboardItem> = emptyMap(),
    val tutorMessage: String? = null,
    val hint: String? = null,
    val textInput: String = "",
    val flowState: WhiteboardFlowState = WhiteboardFlowState.INITIAL,
    val initialProblemStatement: String = "",
    val showConfirmationFailureMessage: Boolean = false,
    val isModelLoading: Boolean = true,
    val selectedImageUri: Uri? = null
)