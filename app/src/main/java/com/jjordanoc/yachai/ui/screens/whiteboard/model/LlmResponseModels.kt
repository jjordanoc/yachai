package com.jjordanoc.yachai.ui.screens.whiteboard.model

import com.jjordanoc.yachai.ui.screens.whiteboard.serialization.LenientStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SideLengths(
    @SerialName("AC") @Serializable(with = LenientStringSerializer::class) val ac: String?,
    @SerialName("AB") @Serializable(with = LenientStringSerializer::class) val ab: String?,
    @SerialName("BC") @Serializable(with = LenientStringSerializer::class) val bc: String?
)

@Serializable
data class AnimationArgs(
    @SerialName("AB") @Serializable(with = LenientStringSerializer::class) val ab: String? = null,
    @SerialName("BC") @Serializable(with = LenientStringSerializer::class) val bc: String? = null,
    @SerialName("AC") @Serializable(with = LenientStringSerializer::class) val ac: String? = null,
    @SerialName("angle_A") @Serializable(with = LenientStringSerializer::class) val angleA: String? = null,
    @SerialName("angle_C") @Serializable(with = LenientStringSerializer::class) val angleC: String? = null,
    val point: String? = null,
    val type: String? = null,
    val segment: String? = null,
    val label: String? = null,
    val expression: String? = null,
    val range: List<Int>? = null,
    val marks: List<Int>? = null,
    val highlight: List<Int>? = null
)

@Serializable
data class AnimationCommand(
    val command: String,
    val args: AnimationArgs
)

@Serializable
data class InterpretResponse(
    @SerialName("problem_type") val problemType: String?,
    @SerialName("tutor_message") val tutorMessage: String?,
    val command: String?,
    val args: AnimationArgs?
)

@Serializable
data class LlmResponse(
    @SerialName("tutor_message") val tutorMessage: String?,
    val hint: String?,
    val animation: List<AnimationCommand> = emptyList()
)
