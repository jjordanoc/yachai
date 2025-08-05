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
    val highlight: List<Int>? = null,
    // Data visualization arguments
    val headers: List<String>? = null,
    val rows: List<List<String>>? = null,
    val categories: List<String>? = null,
    val counts: List<Int>? = null,
    val labels: List<String>? = null,
    val values: List<Int>? = null,
    val min: Int? = null,
    val max: Int? = null,
    val index: Int? = null,
    val summary: String? = null,
    val value: Double? = null,
    // Rectangle animation arguments
    val length: Int? = null,
    val width: Int? = null,
    @Serializable(with = LenientStringSerializer::class) val base: String? = null,
    @Serializable(with = LenientStringSerializer::class) val height: String? = null,
    val lengthLabel: String? = null,
    val widthLabel: String? = null,
    // Grid arguments  
    val unit: String? = null
)

@Serializable
data class AnimationCommand(
    val command: String,
    val args: AnimationArgs,
    @SerialName("clear_previous") val clearPrevious: Boolean = false
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
    val hint: String? = null,
    val animation: List<AnimationCommand> = emptyList()
)

@Serializable
data class TutorialStep(
    @SerialName("tutor_message") val tutorMessage: String,
    val animation: AnimationCommand
)

// New multi-step response format
typealias MultiStepResponse = List<TutorialStep>
