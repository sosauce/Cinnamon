package com.sosauce.cinnamon.features.messaging.data.backup

import kotlinx.serialization.Serializable

const val MESSAGE_BACKUP_VERSION = 1

@Serializable
data class MessageBackupFile(
    val version: Int = MESSAGE_BACKUP_VERSION,
    val app: String = "Cinnamon",
    val createdAtMillis: Long,
    val conversations: List<BackupConversation>
)

@Serializable
data class BackupConversation(
    val threadId: Long,
    val participants: List<BackupParticipant>,
    val snippet: String,
    val dateMillis: Long,
    val read: Boolean,
    val messages: List<BackupMessage>
)

@Serializable
data class BackupParticipant(
    val rawNumber: String,
    val displayName: String,
    val photoUriString: String? = null,
    val isBlocked: Boolean = false
)

@Serializable
data class BackupMessage(
    val id: Long,
    val body: String,
    val type: String,
    val timestampMillis: Long,
    val read: Boolean,
    val isMms: Boolean,
    val delivered: Boolean,
    val attachment: BackupAttachment? = null
)

@Serializable
data class BackupAttachment(
    val id: Long = 0,
    val body: String = "",
    val details: List<BackupAttachmentDetails> = emptyList()
)

@Serializable
data class BackupAttachmentDetails(
    val id: Long,
    val uri: String,
    val filename: String,
    val attachmentType: String,
    val size: Long
)
