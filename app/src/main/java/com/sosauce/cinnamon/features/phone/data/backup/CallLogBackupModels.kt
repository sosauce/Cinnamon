package com.sosauce.cinnamon.features.phone.data.backup

import kotlinx.serialization.Serializable

const val CALL_LOG_BACKUP_VERSION = 1

@Serializable
data class CallLogBackupFile(
    val version: Int = CALL_LOG_BACKUP_VERSION,
    val app: String = "Cinnamon",
    val createdAtMillis: Long,
    val calls: List<BackupCallLog>
)

@Serializable
data class BackupCallLog(
    val number: String,
    val cachedName: String? = null,
    val dateMillis: Long,
    val durationSeconds: Long,
    val type: Int,
    val presentation: Int,
    val location: String? = null
)
