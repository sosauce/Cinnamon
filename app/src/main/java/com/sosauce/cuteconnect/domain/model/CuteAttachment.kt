package com.sosauce.cuteconnect.domain.model

import android.net.Uri
import android.provider.Telephony
import kotlinx.serialization.Serializable

data class CuteAttachment(
    val id: Long = 0,
    val body: String = "",
    val attachmentDetails: List<AttachmentDetails> = emptyList()
) {
    data class AttachmentDetails(
        val id: Long,
        val uri: Uri,
        val filename: String
    )
}

