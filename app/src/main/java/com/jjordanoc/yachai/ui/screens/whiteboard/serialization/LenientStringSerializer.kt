package com.jjordanoc.yachai.ui.screens.whiteboard.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object LenientStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonInput = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonInput.decodeJsonElement()

        if (element is JsonNull) {
            return null
        }

        if (element is JsonPrimitive) {
            return element.content
        }

        return element.toString()
    }
}