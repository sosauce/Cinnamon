package com.sosauce.cinnamon.core

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Uri
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Uri = decoder.decodeString().toUri()
}

object NullableUriSerializer : KSerializer<Uri?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NullableUri", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri?) =
        encoder.encodeString(value?.toString().orEmpty())

    override fun deserialize(decoder: Decoder): Uri? =
        decoder.decodeString().ifEmpty { null }?.toUri()
}