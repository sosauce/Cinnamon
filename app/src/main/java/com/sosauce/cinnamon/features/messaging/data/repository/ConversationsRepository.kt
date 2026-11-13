@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.BlockedNumberContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.PhoneLookup
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.MmsSms
import android.telephony.PhoneNumberUtils
import androidx.compose.ui.util.fastMap
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.messaging.data.model.CuteConversationEntity
import com.sosauce.cinnamon.features.messaging.data.model.MessagesPage
import com.sosauce.cinnamon.features.messaging.domain.Participant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext


class ConversationsRepository(private val context: Context) {


    fun fetchLatestParticipantsForThreadId(threadId: Long) = context.contentResolver.observe(Telephony.Threads.CONTENT_URI)
        .mapLatest { fetchParticipantsForThreadId(threadId) }
        .flowOn(Dispatchers.IO)


    private fun fetchParticipantsForThreadId(threadId: Long): List<Participant> {

        val uri = Telephony.Threads.CONTENT_URI
            .buildUpon()
            .appendQueryParameter("simple", "true")
            .build()

        val projection = arrayOf(Telephony.Threads.RECIPIENT_IDS)


        context.contentResolver.query(
            uri,
            projection,
            "${Telephony.Threads._ID} = ?",
            arrayOf(threadId.toString()),
            null,
        )?.use { cursor ->
            val recipientIdColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)

            if (cursor.moveToFirst()) {

                val participants = cursor.getString(recipientIdColumn)
                    .split(" ")
                    .fastMap { it.getNumberForId() }
                    .fastMap { getParticipantDetails(it) }

                return participants
            }
        }

        return emptyList()
    }

    fun fetchLatestConversations(
        extraSelection: String? = null,
        extraSelectionArgs: Array<String> = emptyArray()
    ): Flow<List<CuteConversationEntity>> {
        return context.contentResolver.observe(Telephony.Threads.CONTENT_URI).mapLatest {
            fetchConversations(extraSelection, extraSelectionArgs)
        }.flowOn(Dispatchers.IO)
    }


    fun fetchConversations(
        extraSelection: String?,
        extraSelectionArgs: Array<String>
    ): List<CuteConversationEntity> {
        val conversations = mutableListOf<CuteConversationEntity>()

        queryThreads(extraSelection, extraSelectionArgs)?.use { cursor ->
            while (cursor.moveToNext()) {
                conversations.add(cursor.readConversation())
            }
        }

        return conversations
    }

    fun observeConversationChanges(): Flow<Boolean> =
        context.contentResolver.observe(Telephony.Threads.CONTENT_URI)

    /**
     * One page of conversations (newest first), starting at [offset].
     * Used by [ConversationsPagingSource].
     */
    suspend fun getConversationsPage(
        extraSelection: String?,
        offset: Int,
        limit: Int
    ): MessagesPage<CuteConversationEntity> = withContext(Dispatchers.IO) {
        val conversations = mutableListOf<CuteConversationEntity>()
        var totalCount = 0

        queryThreads(extraSelection, emptyArray())?.use { cursor ->
            totalCount = cursor.count
            if (limit > 0 && cursor.moveToPosition(offset)) {
                do {
                    conversations.add(cursor.readConversation())
                } while (conversations.size < limit && cursor.moveToNext())
            }
        }

        MessagesPage(messages = conversations, totalCount = totalCount)
    }

    /**
     * Finds the conversations matching [query], either through a participant (contact name or number),
     * or through their content: every message when [searchMessages] is true, otherwise only the snippet.
     *
     * @return threadId -> amount of matching messages (0 when matched by participant/snippet only)
     */
    suspend fun searchThreads(
        query: String,
        searchMessages: Boolean
    ): Map<Long, Int> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<Long, Int>()
        val like = "%${query.escapeLike()}%"

        // participants
        val threadRecipients = mutableMapOf<Long, List<String>>()
        context.contentResolver.query(
            "${Telephony.Threads.CONTENT_URI}?simple=true".toUri(),
            arrayOf(Telephony.Threads._ID, Telephony.Threads.RECIPIENT_IDS),
            "${Telephony.Threads.MESSAGE_COUNT} > 0",
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                threadRecipients[cursor.getLong(0)] = cursor.getString(1).orEmpty().split(" ")
            }
        }

        val addresses = mutableMapOf<String, String>()
        context.contentResolver.query(
            Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses"),
            arrayOf(Telephony.CanonicalAddressesColumns._ID, Telephony.CanonicalAddressesColumns.ADDRESS),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                addresses[cursor.getString(0)] = cursor.getString(1).orEmpty()
            }
        }

        val contactNumbers = runCatching {
            context.contentResolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.NUMBER),
                "${Phone.DISPLAY_NAME} LIKE ? ESCAPE '\\'",
                arrayOf(like),
                null
            )?.use { cursor ->
                buildList { while (cursor.moveToNext()) cursor.getString(0)?.let(::add) }
            }
        }.getOrNull().orEmpty()

        // "06 12 34" should match "+33612345678"-like addresses, so compare digits only for number-like queries
        val queryDigits = query.filter(Char::isDigit)
        val isNumberQuery = queryDigits.length >= 3 && query.all { it.isDigit() || it in " +-()." }

        threadRecipients.forEach { (threadId, recipientIds) ->
            val matches = recipientIds.any { recipientId ->
                val address = addresses[recipientId] ?: return@any false
                address.contains(query, ignoreCase = true) ||
                        (isNumberQuery && address.filter(Char::isDigit).contains(queryDigits)) ||
                        contactNumbers.any { PhoneNumberUtils.compare(it, address) }
            }
            if (matches) results[threadId] = 0
        }

        // content
        if (searchMessages) {
            countMessageMatches(like).forEach { (threadId, count) ->
                results[threadId] = count
            }
        } else {
            context.contentResolver.query(
                "${Telephony.Threads.CONTENT_URI}?simple=true".toUri(),
                arrayOf(Telephony.Threads._ID),
                "${Telephony.Threads.MESSAGE_COUNT} > 0 AND ${Telephony.Threads.SNIPPET} LIKE ? ESCAPE '\\'",
                arrayOf(like),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) results.putIfAbsent(cursor.getLong(0), 0)
            }
        }

        results
    }

    /**
     * threadId -> amount of SMS/MMS whose text matches the LIKE pattern
     */
    private fun countMessageMatches(like: String): Map<Long, Int> {
        val counts = mutableMapOf<Long, Int>()

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.BODY} LIKE ? ESCAPE '\\'",
            arrayOf(like),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(0)
                counts[threadId] = (counts[threadId] ?: 0) + 1
            }
        }

        val partsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Mms.Part.CONTENT_URI
        } else {
            "content://mms/part".toUri()
        }

        val matchingMmsIds = mutableSetOf<Long>()
        context.contentResolver.query(
            partsUri,
            arrayOf(Mms.Part.MSG_ID),
            "${Mms.Part.CONTENT_TYPE} = 'text/plain' AND ${Mms.Part.TEXT} LIKE ? ESCAPE '\\'",
            arrayOf(like),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) matchingMmsIds.add(cursor.getLong(0))
        }

        if (matchingMmsIds.isNotEmpty()) {
            context.contentResolver.query(
                Mms.CONTENT_URI,
                arrayOf(Mms._ID, Mms.THREAD_ID),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getLong(0) in matchingMmsIds) {
                        val threadId = cursor.getLong(1)
                        counts[threadId] = (counts[threadId] ?: 0) + 1
                    }
                }
            }
        }

        return counts
    }

    private fun String.escapeLike(): String = this
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    private fun queryThreads(
        extraSelection: String?,
        extraSelectionArgs: Array<String>
    ): Cursor? {

        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.READ,
            Telephony.Threads.RECIPIENT_IDS
        )

        val selection = buildString {
            append("${Telephony.Threads.MESSAGE_COUNT} > ?")
            extraSelection?.let {
                append(" AND ")
                append(it)
            }
        }

        val selectionArgs = buildList {
            add("0")
            if (extraSelectionArgs.isNotEmpty()) {
                addAll(extraSelectionArgs)
            }
        }.toTypedArray()

        val s = MmsSms.CONTENT_CONVERSATIONS_URI

        return context.contentResolver.query(
            "${Telephony.Threads.CONTENT_URI}?simple=true".toUri(),
            projection,
            selection,
            selectionArgs,
            "${Telephony.Threads.DATE} DESC",
        )
    }

    private fun Cursor.readConversation(): CuteConversationEntity {
        val threadId = getLong(getColumnIndexOrThrow(Telephony.Threads._ID))
        val date = getLong(getColumnIndexOrThrow(Telephony.Threads.DATE))
        val read = getInt(getColumnIndexOrThrow(Telephony.Threads.READ)) != 0
        val systemSnippet = getString(getColumnIndexOrThrow(Telephony.Threads.SNIPPET))
        val snippet = if (systemSnippet.isNullOrBlank()) {
            getMmsThreadSnippet(threadId)
        } else {
            systemSnippet
        }
        val participants = getString(getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS))
            .split(" ")
            .fastMap { it.getNumberForId() }
            .fastMap { getParticipantDetails(it) }

        return CuteConversationEntity(
            threadId = threadId,
            participants = participants,
            snippet = snippet,
            date = date,
            unreadCount = if (read) 0 else getUnreadCount(threadId)
        )
    }


    private fun getParticipantDetails(number: String): Participant {
        if (number.isEmpty()) return Participant(
            rawNumber = number,
            displayName = "<unknown>",
            photoUriString = null,
            isBlocked = false
        )

        val uri = Uri.withAppendedPath(
            PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        val projection = arrayOf(
            PhoneLookup.DISPLAY_NAME,
            PhoneLookup.PHOTO_THUMBNAIL_URI,
            PhoneLookup.CONTACT_ID
        )

        val isBlocked = BlockedNumberContract.canCurrentUserBlockNumbers(context) && BlockedNumberContract.isBlocked(context, number)

        context.contentResolver.query(
            uri,
            projection,
            null,
            null
        )?.use { cursor ->

            val displayNameColumn = cursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME)
            val photoThumbnailColumn = cursor.getColumnIndexOrThrow(PhoneLookup.PHOTO_THUMBNAIL_URI)

            if (cursor.moveToFirst()) {

                val displayName = cursor.getString(displayNameColumn) ?: number.beautifyNumber()
                val photoThumbnail = cursor.getString(photoThumbnailColumn)
                val contactIdColumn = cursor.getColumnIndexOrThrow(PhoneLookup.CONTACT_ID)

                val participant = Participant(
                    rawNumber = number,
                    displayName = displayName,
                    photoUriString = photoThumbnail,
                    isBlocked = isBlocked,
                    contactId = cursor.getLong(contactIdColumn)
                )
                return participant
            }
        }

        return Participant(
            rawNumber = number,
            displayName = number.beautifyNumber(),
            photoUriString = null,
            isBlocked = isBlocked
        )
    }


    private fun String.getNumberForId(): String {

        val uri = Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses")

        context.contentResolver.query(
            uri,
            arrayOf(Telephony.CanonicalAddressesColumns.ADDRESS),
            "${Mms._ID} = ?",
            arrayOf(this),
            null
        )?.use { cursor ->
            val addressColumn = cursor.getColumnIndexOrThrow(Telephony.CanonicalAddressesColumns.ADDRESS)
            if (cursor.moveToFirst()) {
                val address = cursor.getString(addressColumn)
                return address
            }
        }
        return ""
    }

    private fun getUnreadCount(threadId: Long): Int {


        val threadUri = ContentUris.withAppendedId(
            MmsSms.CONTENT_CONVERSATIONS_URI,
            threadId
        )

        return context.contentResolver.query(
            threadUri,
            arrayOf(BaseColumns._ID),
            "${Telephony.TextBasedSmsColumns.READ} = 0",
            null,
            null
        )?.use { it.count } ?: 0
    }

    private fun getMmsThreadSnippet(threadId: Long): String {
        val latestMmsId = context.contentResolver.query(
            Mms.CONTENT_URI,
            arrayOf(Mms._ID),
            "${Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Mms.DATE} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        } ?: return ""

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Mms.Part.CONTENT_URI
        } else {
            "content://mms/part".toUri()
        }

        val projection = arrayOf(
            Mms.Part.CONTENT_TYPE,
            Mms.Part.TEXT,
        )
        context.contentResolver.query(
            uri,
            projection,
            "${Mms.Part.MSG_ID} = ?",
            arrayOf(latestMmsId.toString()),
            "${Mms.Part.SEQ} ASC",
        )?.use { cursor ->
            var fallback = ""
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_TYPE))
                fallback = when {
                    mimeType == "text/plain" -> {
                        return cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.TEXT)) ?: ""
                    }

                    mimeType.startsWith("image/") -> context.getString(R.string.image)
                    mimeType.startsWith("video/") -> context.getString(R.string.video)
                    else -> context.getString(R.string.attachment)
                }
            }
            return fallback
        }

        return ""
    }

    suspend fun deleteThreads(threadIds: List<Long>) = withContext(Dispatchers.IO) {


        val placeholders = threadIds.joinToString(", ") { "?" }
        val stringThreadIds = threadIds.fastMap { it.toString() }.toTypedArray()

        context.contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms.THREAD_ID} IN ($placeholders)",
            stringThreadIds
        )
        context.contentResolver.delete(
            Mms.CONTENT_URI,
            "${Mms.THREAD_ID} IN ($placeholders)",
            stringThreadIds
        )
        context.contentResolver.delete(
            Telephony.Threads.CONTENT_URI,
            "${Telephony.Threads._ID} IN ($placeholders)",
            stringThreadIds
        )
    }

    suspend fun markConversationAsRead(threadId: Long) = withContext(Dispatchers.IO) {
        val smsValues = contentValuesOf(Telephony.Sms.READ to 1)
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            smsValues,
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString())
        )

        val mmsValues = contentValuesOf(Mms.READ to 1)
        context.contentResolver.update(
            Mms.CONTENT_URI,
            mmsValues,
            "${Mms.THREAD_ID} = ? AND ${Mms.READ} = 0",
            arrayOf(threadId.toString())
        )
    }


}
