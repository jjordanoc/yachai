package com.jjordanoc.yachai.ui.screens.whiteboard.animations

data class AnimationSignature(
    val command: String,
    val description: String,
    val args: Map<String, String> // key = argument name, value = description
)
