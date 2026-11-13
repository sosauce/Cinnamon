@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.data.repository

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.BaseColumns
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.MmsSms
import android.provider.Telephony.Sms
import android.util.Xml
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.core.utils.getMMSSize
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.messaging.data.model.AttachmentType
import com.sosauce.cinnamon.features.messaging.data.model.CuteAttachment
import com.sosauce.cinnamon.features.messaging.data.model.CuteMessageEntity
import com.sosauce.cinnamon.features.messaging.data.model.MessagesPage
import com.sosauce.cinnamon.features.messaging.data.model.SearchableMessage
import com.sosauce.cinnamon.features.messaging.data.model.toCuteMessage
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

private const val MMS_STATUS_RETRIEVED = 0x80

class MessagesRepository(
    private val context: Context
) {
    fun fetchLatestSmsForThread(threadId: Long): Flow<List<CuteMessage>> {
        return context.contentResolver.observe(Sms.CONTENT_URI).mapLatest {
            fetchSmsForThread(threadId).fastMap { it.toCuteMessage(context) }
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestMmsForThread(threadId: Long): Flow<List<CuteMessage>> {
        return context.contentResolver.observe(MmsSms.CONTENT_URI).mapLatest {
            fetchMmsForThread(threadId).fastMap { it.toCuteMessage(context) }
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestMessagesForThread(threadId: Long): Flow<List<CuteMessage>> {
        return context.contentResolver
            .observe(MmsSms.CONTENT_URI)
            .mapLatest {
                fetchMessagesForThread(threadId)
                    .fastMap { it.toCuteMessage(context) }
            }
            .flowOn(Dispatchers.IO)
    }

    private fun fetchMessagesForThread(
        threadId: Long
    ): List<CuteMessageEntity> = queryThreadMessages(threadId, 0, Int.MAX_VALUE).messages

    /**
     * Emits whenever anything in the SMS/MMS providers changes (and once on collection).
     */
    fun observeMessageChanges(): Flow<Boolean> =
        context.contentResolver.observe(MmsSms.CONTENT_URI)

    /**
     * One page of SMS+MMS messages for a thread, newest first, starting at [offset].
     * Used by [ThreadMessagesPagingSource].
     */
    suspend fun getMessagesPage(
        threadId: Long,
        offset: Int,
        limit: Int
    ): MessagesPage<CuteMessage> = withContext(Dispatchers.IO) {
        val page = queryThreadMessages(threadId, offset, limit)
        MessagesPage(
            messages = page.messages.fastMap { it.toCuteMessage(context) },
            totalCount = page.totalCount
        )
    }

    /**
     * Lightweight text-only snapshot of a thread, in the same order (newest first) as
     * [getMessagesPage], so that a list index here equals the paged position.
     */
    suspend fun getSearchableMessages(threadId: Long): List<SearchableMessage> =
        withContext(Dispatchers.IO) {
            data class Row(val id: Long, val isMms: Boolean, val body: String?)

            val rows = mutableListOf<Row>()

            context.contentResolver.query(
                ContentUris.withAppendedId(MmsSms.CONTENT_CONVERSATIONS_URI, threadId),
                arrayOf(BaseColumns._ID, Sms.BODY, MmsSms.TYPE_DISCRIMINATOR_COLUMN),
                null,
                null,
                "$NORMALIZED_DATE DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
                val bodyColumn = cursor.getColumnIndexOrThrow(Sms.BODY)
                val transportColumn = cursor.getColumnIndexOrThrow(MmsSms.TYPE_DISCRIMINATOR_COLUMN)

                while (cursor.moveToNext()) {
                    rows.add(
                        Row(
                            id = cursor.getLong(idColumn),
                            isMms = cursor.getString(transportColumn) == "mms",
                            body = cursor.getString(bodyColumn)
                        )
                    )
                }
            }

            val mmsTexts = getMmsTexts(
                rows.fastFilter { it.isMms }.fastMap { it.id }
            )

            rows.fastMap { row ->
                SearchableMessage(
                    id = row.id,
                    isMms = row.isMms,
                    text = if (row.isMms) mmsTexts[row.id].orEmpty() else row.body.orEmpty()
                )
            }
        }

    /**
     * Images and videos of every MMS in a thread, newest message first.
     */
    fun fetchLatestMediaForThread(threadId: Long): Flow<List<CuteAttachment.AttachmentDetails>> {
        return context.contentResolver
            .observe(MmsSms.CONTENT_URI)
            .mapLatest { fetchMediaForThread(threadId) }
            .flowOn(Dispatchers.IO)
    }

    private fun fetchMediaForThread(threadId: Long): List<CuteAttachment.AttachmentDetails> {
        val mmsIds = mutableListOf<Long>()

        context.contentResolver.query(
            Mms.CONTENT_URI,
            arrayOf(Mms._ID),
            "${Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            Mms.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Mms._ID)
            while (cursor.moveToNext()) mmsIds.add(cursor.getLong(idColumn))
        }

        if (mmsIds.isEmpty()) return emptyList()

        val uri = partsUri()
        val idToMedia = mutableMapOf<Long, MutableList<CuteAttachment.AttachmentDetails>>()

        mmsIds.chunked(SQL_IN_CHUNK).fastForEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            context.contentResolver.query(
                uri,
                arrayOf(Mms.Part._ID, Mms.Part.MSG_ID, Mms.Part.CONTENT_TYPE),
                "${Mms.Part.MSG_ID} IN ($placeholders) AND " +
                        "(${Mms.Part.CONTENT_TYPE} LIKE 'image/%' OR ${Mms.Part.CONTENT_TYPE} LIKE 'video/%')",
                chunk.fastMap { it.toString() }.toTypedArray(),
                "${Mms.Part._ID} ASC"
            )?.use { cursor ->
                val partIdColumn = cursor.getColumnIndexOrThrow(Mms.Part._ID)
                val messageIdColumn = cursor.getColumnIndexOrThrow(Mms.Part.MSG_ID)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_TYPE)

                while (cursor.moveToNext()) {
                    val partId = cursor.getLong(partIdColumn)
                    val mimeType = cursor.getString(mimeTypeColumn).orEmpty()
                    idToMedia.getOrPut(cursor.getLong(messageIdColumn)) { mutableListOf() }.add(
                        CuteAttachment.AttachmentDetails(
                            id = partId,
                            uri = ContentUris.withAppendedId(uri, partId),
                            filename = context.getString(R.string.unknown),
                            attachmentType = if (mimeType.startsWith("video/")) AttachmentType.VIDEO else AttachmentType.IMAGE,
                            size = 0
                        )
                    )
                }
            }
        }

        return mmsIds.flatMap { idToMedia[it].orEmpty() }
    }

    /**
     * Maps MMS id -> its text/plain part.
     */
    private fun getMmsTexts(mmsIds: List<Long>): Map<Long, String> {
        if (mmsIds.isEmpty()) return emptyMap()

        val texts = mutableMapOf<Long, String>()

        mmsIds.chunked(SQL_IN_CHUNK).fastForEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            context.contentResolver.query(
                partsUri(),
                arrayOf(Mms.Part.MSG_ID, Mms.Part.TEXT),
                "${Mms.Part.MSG_ID} IN ($placeholders) AND ${Mms.Part.CONTENT_TYPE} = 'text/plain'",
                chunk.fastMap { it.toString() }.toTypedArray(),
                null
            )?.use { cursor ->
                val messageIdColumn = cursor.getColumnIndexOrThrow(Mms.Part.MSG_ID)
                val textColumn = cursor.getColumnIndexOrThrow(Mms.Part.TEXT)
                while (cursor.moveToNext()) {
                    texts[cursor.getLong(messageIdColumn)] = cursor.getString(textColumn).orEmpty()
                }
            }
        }

        return texts
    }

    private fun partsUri() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Mms.Part.CONTENT_URI
    } else {
        "content://mms/part".toUri()
    }

    /**
     * Reads at most [limit] SMS+MMS rows of a thread (newest first), starting at [offset].
     * The cursor is positioned directly instead of using LIMIT/OFFSET because the
     * mms-sms provider doesn't officially support them.
     */
    private fun queryThreadMessages(
        threadId: Long,
        offset: Int,
        limit: Int
    ): MessagesPage<CuteMessageEntity> {
        //if (!PermissionUtils.hasSmsPermission(context)) return emptyList()

        data class MessageRow(
            val id: Long,
            val isMms: Boolean,
            val type: Int,
            val dateMillis: Long,
            val read: Boolean,
            val delivered: Boolean,
            val body: String?
        )

        val rows = mutableListOf<MessageRow>()
        var totalCount = 0

        val projection = arrayOf(
            BaseColumns._ID,
            Sms.THREAD_ID,
            Sms.DATE,
            Sms.BODY,
            Sms.TYPE,
            Sms.READ,
            Sms.STATUS,
            Mms.MESSAGE_BOX,
            MmsSms.TYPE_DISCRIMINATOR_COLUMN
        )

        val conversationUri = ContentUris.withAppendedId(
            MmsSms.CONTENT_CONVERSATIONS_URI,
            threadId
        )

        context.contentResolver.query(
            conversationUri,
            projection,
            null,
            null,
            "$NORMALIZED_DATE DESC"
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(Sms.DATE)
            val bodyColumn = cursor.getColumnIndexOrThrow(Sms.BODY)
            val smsTypeColumn = cursor.getColumnIndexOrThrow(Sms.TYPE)
            val mmsTypeColumn = cursor.getColumnIndexOrThrow(Mms.MESSAGE_BOX)
            val readColumn = cursor.getColumnIndexOrThrow(Sms.READ)
            val statusColumn = cursor.getColumnIndexOrThrow(Sms.STATUS)
            val transportColumn = cursor.getColumnIndexOrThrow(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN
            )

            totalCount = cursor.count

            if (limit > 0 && cursor.moveToPosition(offset)) {
                do {
                val isMms = cursor.getString(transportColumn) == "mms"

                val id = cursor.getLong(idColumn)
                val rawDate = cursor.getLong(dateColumn)

                // SMS dates are milliseconds, MMS dates are seconds.
                val dateMillis = if (isMms) {
                    if (rawDate > 0L) rawDate * 1000L
                    else System.currentTimeMillis()
                } else {
                    rawDate
                }

                val type = cursor.getInt(
                    if (isMms) mmsTypeColumn else smsTypeColumn
                )

                val read = cursor.getInt(readColumn) == 1
                val status = cursor.getInt(statusColumn)

                val delivered = if (isMms) {
                    !cursor.isNull(statusColumn) &&
                            status == MMS_STATUS_RETRIEVED
                } else {
                    status == Sms.STATUS_COMPLETE
                }

                rows.add(
                    MessageRow(
                        id = id,
                        isMms = isMms,
                        type = type,
                        dateMillis = dateMillis,
                        read = read,
                        delivered = delivered,
                        body = cursor.getString(bodyColumn)
                    )
                )
            } while (rows.size < limit && cursor.moveToNext())
            }
        }

        // Fetch attachments for all MMS messages in a single query.
        val mmsIds = rows
            .fastFilter { it.isMms }
            .fastMap { it.id }

        val allMmsAttachments = if (mmsIds.isNotEmpty()) {
            getAllMmsAttachment(mmsIds)
        } else {
            emptyMap()
        }

        val messages = rows.fastMap { row ->
            val attachment = if (row.isMms) {
                allMmsAttachments[row.id]
            } else {
                null
            }

            CuteMessageEntity(
                id = row.id,
                body = attachment?.body ?: row.body.orEmpty(),
                type = row.type,
                threadId = threadId,
                read = row.read,
                delivered = row.delivered,
                isMms = row.isMms,
                attachment = attachment,
                dateMillis = row.dateMillis,
                isScheduled = false
            )
        }

        return MessagesPage(messages = messages, totalCount = totalCount)
    }

    /**
     * One-shot SMS+MMS snapshot for a thread, sorted by timestamp.
     * Used by the conversation backup/export flow.
     */
    suspend fun getMessagesForThread(threadId: Long): List<CuteMessage> =
        withContext(Dispatchers.IO) {
            fetchMessagesForThread(threadId)
                .fastMap { it.toCuteMessage(context) }
        }



    private fun fetchSmsForThread(threadId: Long): List<CuteMessageEntity> {

        if (!PermissionUtils.hasSmsPermission(context)) return emptyList()


        val messages = mutableListOf<CuteMessageEntity>()

        val projection = arrayOf(
            Sms._ID,
            Sms.THREAD_ID,
            Sms.DATE,
            Sms.BODY,
            Sms.TYPE,
            Sms.READ,
            Sms.STATUS
        )

        val selection = "${Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        context.contentResolver.query(
            Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Sms.DEFAULT_SORT_ORDER,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Sms._ID)
            val threadIdColumn = cursor.getColumnIndexOrThrow(Sms.THREAD_ID)
            val dateColumn = cursor.getColumnIndexOrThrow(Sms.DATE)
            val bodyColumn = cursor.getColumnIndexOrThrow(Sms.BODY)
            val typeColumn = cursor.getColumnIndexOrThrow(Sms.TYPE)
            val readColumn = cursor.getColumnIndexOrThrow(Sms.READ)
            val statusColumn = cursor.getColumnIndexOrThrow(Sms.STATUS)


            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)
                val body = cursor.getString(bodyColumn)
                val type = cursor.getInt(typeColumn)
                val threadId = cursor.getLong(threadIdColumn)
                val date = cursor.getLong(dateColumn)
                val read = cursor.getInt(readColumn) == 1
                val delivered = cursor.getInt(statusColumn) == Sms.STATUS_COMPLETE

                val message = CuteMessageEntity(
                    id = id,
                    body = body,
                    type = type,
                    threadId = threadId,
                    dateMillis = date,
                    read = read,
                    delivered = delivered,
                    isMms = false,
                    isScheduled = false
                )


                messages.add(message)
            }
        }
        return messages
    }

    private fun fetchMmsForThread(threadId: Long): List<CuteMessageEntity> {

        val projection = arrayOf(
            Mms._ID,
            Mms.MESSAGE_BOX,
            Mms.READ,
            Mms.DATE,
            Mms.TEXT_ONLY,
            Mms.STATUS // this is what smsmms updates for MMS delivery reports
        )

        val selection = "${Mms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        data class Row(
            val id: Long,
            val type: Int,
            val date: Long,
            val read: Boolean,
            val delivered: Boolean
        )

        val rows = mutableListOf<Row>()


        context.contentResolver.query(
            Mms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Mms.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Mms._ID)
            val typeColumn = cursor.getColumnIndexOrThrow(Mms.MESSAGE_BOX)
            val dateColumn = cursor.getColumnIndexOrThrow(Mms.DATE)
            val readColumn = cursor.getColumnIndexOrThrow(Mms.READ)
            val statusColumn = cursor.getColumnIndexOrThrow(Mms.STATUS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val type = cursor.getInt(typeColumn)
                val rawDate = cursor.getLong(dateColumn)
                val date = if (rawDate > 1_000L) rawDate * 1000 else System.currentTimeMillis()
                val read = cursor.getInt(readColumn) == 1
                val delivered = !cursor.isNull(statusColumn) &&
                    cursor.getInt(statusColumn) == MMS_STATUS_RETRIEVED

                rows.add(
                    Row(
                        id = id,
                        date = date,
                        read = read,
                        type = type,
                        delivered = delivered
                    )
                )
            }
        }

        if (rows.isEmpty()) return emptyList()

        val ids = rows.fastMap { it.id }
        val allMmsAttachment = getAllMmsAttachment(ids)


        return rows.fastMap { row ->
            val attachment = allMmsAttachment[row.id]

            CuteMessageEntity(
                id = row.id,
                body = attachment?.body.orEmpty(),
                type = row.type,
                threadId = threadId,
                read = row.read,
                delivered = row.delivered,
                isMms = true,
                attachment = attachment,
                dateMillis = row.date,
                isScheduled = false
            )
        }
    }


    private fun getAllMmsAttachment(
        mmsIds: List<Long>,
    ): Map<Long, CuteAttachment> {

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Mms.Part.CONTENT_URI
        } else {
            "content://mms/part".toUri()
        }


        val projection = arrayOf(
            Mms.Part._ID,
            Mms.Part.MSG_ID,
            Mms.Part.CONTENT_TYPE,
            Mms.Part.TEXT
        )
        val placeholders = mmsIds.joinToString(",") { "?" }
        val selection = "${Mms.Part.MSG_ID} in ($placeholders)"


        data class RawPart(
            val partId: Long,
            val mimeType: String,
            val text: String
        )

        val idToParts = mutableMapOf<Long, MutableList<RawPart>>()



        context.contentResolver.query(
            uri,
            projection,
            selection,
            mmsIds.fastMap { it.toString() }.toTypedArray(),
            null
        )?.use { cursor ->

            val partIdColumn = cursor.getColumnIndexOrThrow(Mms._ID)
            val messageIdColumn = cursor.getColumnIndexOrThrow(Mms.Part.MSG_ID)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_TYPE)
            val textColumn = cursor.getColumnIndexOrThrow(Mms.Part.TEXT)

            while (cursor.moveToNext()) {
                val partId = cursor.getLong(partIdColumn)
                val messageId = cursor.getLong(messageIdColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val text = cursor.getString(textColumn) ?: ""

                idToParts.getOrPut(messageId) { mutableListOf() }.add(
                    RawPart(
                        partId = partId,
                        mimeType = mimeType,
                        text = text
                    )
                )

            }
        }

        return idToParts.mapValues { (id, parts) ->
            var body = ""
            val attachmentDetails = mutableListOf<CuteAttachment.AttachmentDetails>()
            var filename = ""

            parts.fastForEach { part ->
                val fileUri = ContentUris.withAppendedId(uri, part.partId)

                when {
                    part.mimeType == "text/plain" -> body = part.text
                    part.mimeType.startsWith("image/") -> {

                        attachmentDetails.add(
                            CuteAttachment.AttachmentDetails(
                                id = part.partId,
                                uri = fileUri,
                                filename = context.getString(R.string.unknown), // TODO get filename
                                attachmentType = AttachmentType.IMAGE,
                                size = 0 // we don't care about the size for known filetype since we display them and not info, saves resources
                            )
                        )
                    }

                    part.mimeType.startsWith("video/") -> {

                        attachmentDetails.add(
                            CuteAttachment.AttachmentDetails(
                                id = part.partId,
                                uri = fileUri,
                                filename = context.getString(R.string.unknown),
                                attachmentType = AttachmentType.VIDEO,
                                size = 0
                            )
                        )
                    }

                    part.mimeType.startsWith("audio/") -> {

                        attachmentDetails.add(
                            CuteAttachment.AttachmentDetails(
                                id = part.partId,
                                uri = fileUri,
                                filename = context.getString(R.string.unknown),
                                attachmentType = AttachmentType.AUDIO,
                                size = 0
                            )
                        )
                    }

                    part.mimeType == "application/smil" -> {
                        filename = parseAttachmentNames(part.text).firstOrNull() ?: "idk"
                    }

                    else -> {
                        val size = context.getMMSSize(fileUri)
                        attachmentDetails.add(
                            CuteAttachment.AttachmentDetails(
                                id = part.partId,
                                uri = fileUri,
                                filename = filename,
                                attachmentType = AttachmentType.OTHER,
                                size = size
                            )
                        )
                    }

                }
            }

            CuteAttachment(
                id = id,
                body = body,
                attachmentDetails = attachmentDetails
            )
        }
    }

    private fun parseAttachmentNames(text: String): List<String> {

        if (text.isBlank()) return emptyList()

        return runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(text.reader())
            parser.nextTag()
            readSmil(parser)
        }.getOrDefault(emptyList())
    }

    private fun readSmil(parser: XmlPullParser): List<String> {
        parser.require(XmlPullParser.START_TAG, null, "smil")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            if (parser.name == "body") {
                return readBody(parser)
            } else {
                skip(parser)
            }
        }

        return emptyList()
    }

    private fun readBody(parser: XmlPullParser): List<String> {
        val names = mutableListOf<String>()
        parser.require(XmlPullParser.START_TAG, null, "body")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            if (parser.name == "par") {
                parser.require(XmlPullParser.START_TAG, null, "par")
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.eventType != XmlPullParser.START_TAG) {
                        continue
                    }

                    if (parser.name in listOf("img", "audio", "video", "vcard", "ref")) {
                        parser.getAttributeValue(null, "src")?.let(names::add)
                        skip(parser)
                    } else {
                        skip(parser)
                    }
                }
            } else {
                skip(parser)
            }
        }
        return names
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }

        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    suspend fun deleteConversation(threadId: Long) = withContext(Dispatchers.IO) {
        context.contentResolver.delete(
            Sms.CONTENT_URI,
            "${Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString())
        )
        context.contentResolver.delete(
            Mms.CONTENT_URI,
            "${Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString())
        )
    }

    suspend fun deleteMessages(messages: List<CuteMessage>) = withContext(Dispatchers.IO) {

        val smsOps = ArrayList<ContentProviderOperation>()
        val mmsOps = ArrayList<ContentProviderOperation>()

        messages.fastFilter { !it.isScheduled }.fastForEach { message ->

            if (message.isMms) {
                mmsOps.add(
                    ContentProviderOperation.newDelete(Mms.CONTENT_URI)
                        .withSelection("${Mms._ID} = ?", arrayOf(message.id.toString()))
                        .build()
                )
            } else {
                smsOps.add(
                    ContentProviderOperation.newDelete(Sms.CONTENT_URI)
                        .withSelection("${Sms._ID} = ?", arrayOf(message.id.toString()))
                        .build()
                )
            }
        }

        if (smsOps.isNotEmpty()) {
            context.contentResolver.applyBatch("sms", smsOps)
        }

        if (mmsOps.isNotEmpty()) {
            context.contentResolver.applyBatch("mms", mmsOps)
        }
    }

    companion object {
        private const val NORMALIZED_DATE = "normalized_date"
        // stay well below SQLite's bound-variable limit on older devices (999)
        private const val SQL_IN_CHUNK = 900
    }

}
