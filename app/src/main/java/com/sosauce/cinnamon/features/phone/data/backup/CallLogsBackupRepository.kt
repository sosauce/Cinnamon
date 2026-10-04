package com.sosauce.cinnamon.features.phone.data.backup

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.phone.data.model.CuteCallLogEntity
import com.sosauce.cinnamon.features.phone.data.repository.CallLogsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a JSON-serializable snapshot of the selected call log entries and
 * persists it through the Storage Access Framework [Uri] supplied by the UI.
 *
 * Only provider-owned columns are exported. Display-oriented cached columns
 * (photo, lookup keys) are intentionally omitted: the system recomputes them
 * from the contacts database on read. Numbers for non-allowed presentations
 * (private/payphone/unknown) are stored blank with their presentation intact.
 */
class CallLogsBackupRepository(
    private val context: Context,
    private val callLogsRepository: CallLogsRepository
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun buildBackup(
        logIds: Set<Long>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): CallLogBackupFile = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasCallLogReadPermission(context)) {
            throw SecurityException(context.getString(R.string.backup_needs_call_log_permission))
        }

        val entities = callLogsRepository.fetchCallLogs()
            .filter { it.id in logIds }

        val calls = ArrayList<BackupCallLog>(entities.size)
        entities.forEachIndexed { index, entity ->
            calls.add(entity.toBackupCall())
            if (entities.isNotEmpty()) {
                onProgress(index + 1, entities.size)
            }
        }

        CallLogBackupFile(
            createdAtMillis = System.currentTimeMillis(),
            calls = calls
        )
    }

    suspend fun writeBackupToUri(destination: Uri, backup: CallLogBackupFile) =
        withContext(Dispatchers.IO) {
            val payload = json.encodeToString(CallLogBackupFile.serializer(), backup)
            context.contentResolver.openOutputStream(destination, "wt")?.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Unable to open output stream for backup destination")
        }

    fun defaultFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        return "calls_${formatter.format(Date(nowMillis))}.json"
    }

    fun sanitizeFileName(raw: String, fallback: String = defaultFileName()): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return fallback
        var name = trimmed.replace('/', '_').replace('\\', '_')
        if (!name.endsWith(".json", ignoreCase = true)) {
            name += ".json"
        }
        return name
    }

    private fun CuteCallLogEntity.toBackupCall(): BackupCallLog {
        return BackupCallLog(
            number = if (presentation == CallLog.Calls.PRESENTATION_ALLOWED) number else "",
            cachedName = cachedName?.takeIf { it.isNotBlank() },
            dateMillis = date,
            durationSeconds = duration,
            type = type,
            presentation = presentation,
            location = location?.takeIf { it.isNotBlank() }
        )
    }
}
