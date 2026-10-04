package com.sosauce.cinnamon.features.phone.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.OpenableColumns
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class ImportableCallLog(
    val key: Int,
    val log: BackupCallLog
)

data class CallLogsImportResult(
    val imported: Int,
    val skipped: Int
)

/**
 * Restores call log entries from a JSON backup produced by
 * [CallLogsBackupRepository]. Reads the picked file through SAF, checks for
 * duplicates against the system CallLog provider and inserts on
 * [Dispatchers.IO] with per-entry progress.
 *
 * Only provider-owned columns are written (number, type, date, duration,
 * presentation); cached display columns are recomputed by the system.
 * Inserting requires [android.Manifest.permission.WRITE_CALL_LOG].
 */
class CallLogsImportRepository(
    private val context: Context
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun parseBackup(uri: Uri): List<BackupCallLog> = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasCallLogReadPermission(context)) {
            throw SecurityException(context.getString(R.string.backup_needs_call_log_permission))
        }
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error(context.getString(R.string.import_empty_file))
        json.decodeFromString(CallLogBackupFile.serializer(), text).calls
    }

    suspend fun importCalls(
        items: List<ImportableCallLog>,
        strategy: ImportStrategy,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): CallLogsImportResult = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasCallLogWritePermission(context)) {
            throw SecurityException(context.getString(R.string.import_needs_call_log_permission))
        }

        var imported = 0
        var skipped = 0

        items.forEachIndexed { index, item ->
            val duplicate = strategy == ImportStrategy.SKIP_EXISTING && isDuplicate(item.log)
            if (duplicate) {
                skipped++
            } else {
                val ok = insertCall(item.log)
                if (!ok) error(context.getString(R.string.backup_failed))
                imported++
            }
            if (items.isNotEmpty()) {
                onProgress(index + 1, items.size)
            }
        }

        CallLogsImportResult(imported = imported, skipped = skipped)
    }

    fun readDisplayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: context.getString(R.string.backup_file_name)
    }

    private fun isDuplicate(log: BackupCallLog): Boolean {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID),
            "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ? AND " +
                    "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DURATION} = ? AND " +
                    "${CallLog.Calls.NUMBER_PRESENTATION} = ?",
            arrayOf(
                log.number,
                log.dateMillis.toString(),
                log.type.toString(),
                log.durationSeconds.toString(),
                log.presentation.toString()
            ),
            null
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    private fun insertCall(log: BackupCallLog): Boolean {
        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, log.number)
            put(CallLog.Calls.TYPE, log.type)
            put(CallLog.Calls.DATE, log.dateMillis)
            put(CallLog.Calls.DURATION, log.durationSeconds)
            put(CallLog.Calls.NUMBER_PRESENTATION, log.presentation)
        }
        return context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values) != null
    }
}
