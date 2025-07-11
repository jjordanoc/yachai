package com.jjordanoc.yachai.ui.screens.whiteboard.model

import android.net.Uri
import com.jjordanoc.yachai.ui.screens.whiteboard.WhiteboardFlowState

data class WhiteboardState(
    val items: List<WhiteboardItem> = emptyList(),
    val textInput: String = "",
    val flowState: WhiteboardFlowState = WhiteboardFlowState.INITIAL,
    val initialProblemStatement: String = "",
    val tutorMessage: String? = null,
    val hint: String? = null,
    val expressions: List<String> = emptyList(),
    val showConfirmationFailureMessage: Boolean = false,
    val selectedImageUri: Uri? = null,
    val isModelLoading: Boolean = true
)