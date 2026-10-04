package com.sosauce.cinnamon.features.messaging.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Telephony
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

enum class ImportStrategy {
    SKIP_EXISTING,
    IMPORT_ALL
}

data class SmsImportResult(
    val imported: Int,
    val skipped: Int
)

/**
 * Restores SMS/MMS conversations from a JSON backup produced by
 * [MessageBackupRepository]. Checks for
 * duplicates against the Telephony provider and inserts messages with per-message progress.
 *
 * Writing to the SMS provider requires Cinnamon to be the default SMS app;
 * callers must gate on [isDefaultSmsApp] first.
 *
 * MMS media bytes were never exported (metadata only), so restored MMS
 * entries contain their text bodies; group threads are rebuilt via
 * [Telephony.Threads.getOrCreateThreadId].
 */
class MessagesImportRepository(
    private val context: Context
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(context) == context.packageName



    suspend fun parseBackup(uri: Uri): MessageBackupFile = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error(context.getString(R.string.import_empty_file))
        json.decodeFromString(MessageBackupFile.serializer(), text)
    }

    suspend fun importConversations(
        conversations: List<BackupConversation>,
        strategy: ImportStrategy,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): SmsImportResult = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasSmsPermission(context)) {
            throw SecurityException(context.getString(R.string.backup_needs_sms_permission))
        }
        if (!isDefaultSmsApp()) {
            throw SecurityException(context.getString(R.string.import_needs_default_sms))
        }

        val total = conversations.sumOf { it.messages.size }
        var imported = 0
        var skipped = 0
        var done = 0

        conversations.forEach { conversation ->
            val numbers =
                conversation.participants.mapNotNull { it.rawNumber.takeIf { n -> n.isNotBlank() } }
            val threadId = numbers.takeIf { it.isNotEmpty() }?.let { recipients ->
                Telephony.Threads.getOrCreateThreadId(context, recipients.toSet())
            }

            conversation.messages.forEach { message ->
                val duplicate = strategy == ImportStrategy.SKIP_EXISTING &&
                        if (message.isMms) isMmsDuplicate(threadId, message) else isSmsDuplicate(
                            message,
                            numbers
                        )
                if (duplicate) {
                    skipped++
                } else {
                    val ok = if (message.isMms) {
                        insertMms(message, numbers, threadId)
                    } else {
                        insertSms(message, numbers, threadId)
                    }
                    if (!ok) error(context.getString(R.string.backup_failed))
                    imported++
                }
                done++
                if (total > 0) onProgress(done, total)
            }
        }

        SmsImportResult(imported = imported, skipped = skipped)
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

    private fun isSmsDuplicate(message: BackupMessage, numbers: List<String>): Boolean {
        val address = numbers.firstOrNull() ?: return false
        val type = if (message.type == "RECEIVED") {
            Telephony.Sms.MESSAGE_TYPE_INBOX
        } else {
            Telephony.Sms.MESSAGE_TYPE_SENT
        }
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} = ? AND " +
                    "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.BODY} = ?",
            arrayOf(address, message.timestampMillis.toString(), type.toString(), message.body),
            null
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    private fun isMmsDuplicate(threadId: Long?, message: BackupMessage): Boolean {
        if (threadId == null) return false
        context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms._ID),
            "${Telephony.Mms.THREAD_ID} = ? AND ${Telephony.Mms.DATE} = ?",
            arrayOf(threadId.toString(), (message.timestampMillis / 1000).toString()),
            null
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    private fun insertSms(
        message: BackupMessage,
        numbers: List<String>,
        threadId: Long?
    ): Boolean {
        val address = numbers.firstOrNull() ?: return false
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, message.body)
            put(Telephony.Sms.DATE, message.timestampMillis)
            put(Telephony.Sms.READ, if (message.read) 1 else 0)
            put(Telephony.Sms.SEEN, 1)
            put(
                Telephony.Sms.TYPE,
                if (message.type == "RECEIVED") {
                    Telephony.Sms.MESSAGE_TYPE_INBOX
                } else {
                    Telephony.Sms.MESSAGE_TYPE_SENT
                }
            )
            threadId?.let { put(Telephony.Sms.THREAD_ID, it) }
        }
        return context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values) != null
    }

    private fun insertMms(
        message: BackupMessage,
        numbers: List<String>,
        threadId: Long?
    ): Boolean {
        val isInbox = message.type == "RECEIVED"
        val pdu = ContentValues().apply {
            threadId?.let { put(Telephony.Mms.THREAD_ID, it) }
            put(
                Telephony.Mms.MESSAGE_BOX,
                if (isInbox) Telephony.Mms.MESSAGE_BOX_INBOX else Telephony.Mms.MESSAGE_BOX_SENT
            )
            // "m_type": PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF = 132, MESSAGE_TYPE_SEND_REQ = 128
            put("m_type", if (isInbox) 132 else 128)
            put(Telephony.Mms.DATE, message.timestampMillis / 1000)
            put(Telephony.Mms.READ, if (message.read) 1 else 0)
            put(Telephony.Mms.SEEN, 1)
        }
        val pduUri = context.contentResolver.insert(Telephony.Mms.CONTENT_URI, pdu) ?: return false
        val msgId = ContentUris.parseId(pduUri)

        val addrBase = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "$msgId/addr")
        if (isInbox) {
            // PduHeaders.FROM = 137
            numbers.firstOrNull()?.let { insertMmsAddr(addrBase, msgId, it, 137) }
        } else {
            // PduHeaders.TO = 151
            numbers.forEach { insertMmsAddr(addrBase, msgId, it, 151) }
        }

        val body = message.attachment?.body?.takeIf { it.isNotBlank() } ?: message.body
        if (body.isNotBlank()) {
            val partBase = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "$msgId/part")
            val part = ContentValues().apply {
                put(Telephony.Mms.Part.MSG_ID, msgId)
                put(Telephony.Mms.Part.CONTENT_TYPE, "text/plain")
                // PduHeaders.CHARACTER_SET_UTF_8 = 106
                put(Telephony.Mms.Part.CHARSET, 106)
                put(Telephony.Mms.Part.TEXT, body)
            }
            context.contentResolver.insert(partBase, part)
        }
        return true
    }

    private fun insertMmsAddr(base: Uri, msgId: Long, address: String, type: Int) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Addr.MSG_ID, msgId)
            put(Telephony.Mms.Addr.ADDRESS, address)
            put(Telephony.Mms.Addr.TYPE, type)
            put(Telephony.Mms.Addr.CHARSET, 106)
        }
        context.contentResolver.insert(base, values)
    }
}
