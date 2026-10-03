package com.sosauce.cinnamon.features.messaging.data.backup

import android.content.Context
import android.net.Uri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.data.repository.MessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a JSON-serializable snapshot of the selected conversations and
 * persists it through the Storage Access Framework [Uri] supplied by the UI.
 *
 * MMS media bytes are intentionally NOT embedded: only metadata (type,
 * filename, size, originating content Uri) plus the text body is exported.
 * This keeps backups small, avoids OOM on large threads and avoids needing
 * any storage permission (SAF write only).
 */
class MessageBackupRepository(
    private val context: Context,
    private val conversationsRepository: ConversationsRepository,
    private val messagesRepository: MessagesRepository
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun buildBackup(
        threadIds: Set<Long>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): MessageBackupFile =
        withContext(Dispatchers.IO) {
            if (!PermissionUtils.hasSmsPermission(context)) {
                throw SecurityException(context.getString(R.string.backup_needs_sms_permission))
            }

            val entities = conversationsRepository.fetchConversations(null, emptyArray())
                .filter { it.threadId in threadIds }

            val conversations = ArrayList<BackupConversation>(entities.size)
            entities.forEachIndexed { index, entity ->
                val messages = messagesRepository.getMessagesForThread(entity.threadId)

                conversations.add(
                    BackupConversation(
                        threadId = entity.threadId,
                        participants = entity.participants.map {
                            BackupParticipant(
                                rawNumber = it.rawNumber,
                                displayName = it.displayName,
                                photoUriString = it.photoUriString,
                                isBlocked = it.isBlocked
                            )
                        },
                        snippet = entity.snippet,
                        dateMillis = entity.date,
                        read = entity.read,
                        messages = messages.map { message ->
                            BackupMessage(
                                id = message.id,
                                body = message.body,
                                type = message.type.name,
                                timestampMillis = message.timestamp,
                                read = message.read,
                                isMms = message.isMms,
                                delivered = message.delivered,
                                attachment = message.attachment?.let { attachment ->
                                    BackupAttachment(
                                        id = attachment.id,
                                        body = attachment.body,
                                        details = attachment.attachmentDetails.map { detail ->
                                            BackupAttachmentDetails(
                                                id = detail.id,
                                                uri = detail.uri.toString(),
                                                filename = detail.filename,
                                                attachmentType = detail.attachmentType.name,
                                                size = detail.size
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                )
                if (entities.isNotEmpty()) {
                    onProgress(index + 1, entities.size)
                }
            }

            MessageBackupFile(
                createdAtMillis = System.currentTimeMillis(),
                conversations = conversations
            )
        }

    suspend fun writeBackupToUri(destination: Uri, backup: MessageBackupFile) =
        withContext(Dispatchers.IO) {
            val payload = json.encodeToString(MessageBackupFile.serializer(), backup)
            context.contentResolver.openOutputStream(destination, "wt")?.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Unable to open output stream for backup destination")
        }

    fun defaultFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        return "backup_${formatter.format(Date(nowMillis))}.json"
    }

    fun sanitizeFileName(raw: String, fallback: String = defaultFileName()): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return fallback
        // SAF display names must not contain path separators; replace them.
        var name = trimmed.replace('/', '_').replace('\\', '_')
        if (!name.endsWith(".json", ignoreCase = true)) {
            name += ".json"
        }
        return name
    }
}
