@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.domain.repository

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
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
import com.sosauce.cinnamon.domain.model.AttachmentType
import com.sosauce.cinnamon.domain.model.CuteAttachment
import com.sosauce.cinnamon.domain.model.CuteMessage
import com.sosauce.cinnamon.utils.PermissionUtils
import com.sosauce.cinnamon.utils.getMMSSize
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

class ConversationsRepository(
    private val context: Context
) {
    fun fetchLatestSmsForThread(threadId: Long): Flow<List<CuteMessage>> {
        return context.contentResolver.observe(Sms.CONTENT_URI).mapLatest {
            fetchSmsForThread(threadId)
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestMmsForThread(threadId: Long): Flow<List<CuteMessage>> {
        return context.contentResolver.observe(MmsSms.CONTENT_URI).mapLatest {
            fetchMmsForThread(threadId)
        }.flowOn(Dispatchers.IO)
    }


    private fun fetchSmsForThread(threadId: Long): List<CuteMessage> {

        if (!PermissionUtils.hasSmsPermission(context)) return emptyList()


        val messages = mutableListOf<CuteMessage>()

        val projection = arrayOf(
            Sms._ID,
            Sms.THREAD_ID,
            Sms.ADDRESS,
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
            val addressColumn = cursor.getColumnIndexOrThrow(Sms.ADDRESS)
            val dateColumn = cursor.getColumnIndexOrThrow(Sms.DATE)
            val bodyColumn = cursor.getColumnIndexOrThrow(Sms.BODY)
            val typeColumn = cursor.getColumnIndexOrThrow(Sms.TYPE)
            val readColumn = cursor.getColumnIndexOrThrow(Sms.READ)
            val statusColumn = cursor.getColumnIndexOrThrow(Sms.STATUS)


            while (cursor.moveToNext()) {
                messages.add(
                    CuteMessage(
                        id = cursor.getLong(idColumn),
                        body = cursor.getString(bodyColumn),
                        type = cursor.getInt(typeColumn),
                        threadId = cursor.getLong(threadIdColumn),
                        address = cursor.getString(addressColumn),
                        date = cursor.getLong(dateColumn),
                        read = cursor.getInt(readColumn) == 1,
                        delivered = cursor.getInt(statusColumn) == Sms.STATUS_COMPLETE
                    )
                )
            }
        }
        return messages
    }

    private fun fetchMmsForThread(threadId: Long): List<CuteMessage> {

        val projection = arrayOf(
            Mms._ID,
            Mms.MESSAGE_BOX,
            Mms.READ,
            Mms.DATE,
            //Mms.Addr.ADDRESS,
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
                val delivered = cursor.getInt(statusColumn) == Sms.STATUS_COMPLETE

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

            CuteMessage(
                id = row.id,
                body = attachment?.body ?: "",
                type = row.type,
                address = "", // TODO, get address
                date = row.date,
                threadId = threadId,
                read = row.read,
                delivered = row.delivered,
                isMms = true,
                attachment = attachment
            )
        }
    }

    fun fetchThreadRecipients(threadId: Long): List<String> {
        val recipients = mutableListOf<String>()

        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.RECIPIENT_IDS
        )
        context.contentResolver.query(
            "${Telephony.Threads.CONTENT_URI}?simple=true".toUri(),
            projection,
            "${Telephony.Threads._ID} = ?",
            arrayOf(threadId.toString()),
            null,
        )?.use { cursor ->
            val recipientIdsColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)

            while (cursor.moveToNext()) {

                val recipientIds = cursor.getString(recipientIdsColumn)
                val recipientIdsAsLongs = recipientIds.split(" ").map { it.toLongOrNull() ?: 0 }
                val rawRecipients = recipientIdsAsLongs.fastMap { getPhoneNumberOfRecipientId(it) }
                recipients.addAll(rawRecipients)
            }
        }
        return recipients
    }

    private fun getPhoneNumberOfRecipientId(id: Long): String {
        val uri = Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses")
        val projection = arrayOf(
            Mms.Addr.ADDRESS
        )

        val selection = "${Mms._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                val addressColumn = cursor.getColumnIndexOrThrow(Mms.Addr.ADDRESS)
                if (cursor.moveToFirst()) {
                    return cursor.getString(addressColumn)
                }
            }
        return ""
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
                        text = text,
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
                                size = 0 // we dont care about the size for known filetype since we display them and not info, saves resources
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

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(text.reader())
        parser.nextTag()
        return readSmil(parser)
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
                        names.add(parser.getAttributeValue(null, "src"))
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

}
