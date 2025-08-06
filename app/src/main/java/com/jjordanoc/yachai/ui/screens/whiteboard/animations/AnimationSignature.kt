package com.jjordanoc.yachai.ui.screens.whiteboard.animations

import kotlinx.serialization.Serializable

@Serializable
data class AnimationSignature(
    val command: String,
    val description: String?,
    val args: Map<String, String> // key = argument name, value = description
)
