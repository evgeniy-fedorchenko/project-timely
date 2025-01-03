package com.efedorchenko.timely.model.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.threeten.bp.Duration

object CustomDurationSerializer : KSerializer<Duration> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CustomDuration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeDouble(value.toMillis() / 1000.0)
    }

    override fun deserialize(decoder: Decoder): Duration {
        val seconds = decoder.decodeDouble()
        return Duration.ofMillis((seconds * 1000).toLong())
    }
}