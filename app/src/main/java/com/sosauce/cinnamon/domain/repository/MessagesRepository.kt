@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.domain.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.MmsSms
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteCallLog
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.domain.model.CuteMessage
import com.sosauce.cinnamon.utils.beautifyNumber
import com.sosauce.cinnamon.utils.getContactNameOrNothing
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext


class MessagesRepository(
    private val context: Context,
    private val blockedNumbersManager: BlockedNumbersManager
) {


    fun fetchLatestConversations(
        extraSelection: String? = null,
        extraSelectionArgs: Array<String> = emptyArray(),
        extraSortOrder: String = "${Telephony.Threads.DATE} DESC"
    ): Flow<List<CuteConversation>> {
        return context.contentResolver.observe(Telephony.Threads.CONTENT_URI).mapLatest {
            fetchConversations(extraSelection, extraSelectionArgs, extraSortOrder)
        }.flowOn(Dispatchers.IO)
    }


    private fun fetchConversations(
        extraSelection: String?,
        extraSelectionArgs: Array<String>,
        extraSortOrder: String
    ): List<CuteConversation> {
        val conversations = mutableListOf<CuteConversation>()

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


        context.contentResolver.query(
            "${Telephony.Threads.CONTENT_URI}?simple=true".toUri(),
            projection,
            selection,
            selectionArgs,
            extraSortOrder,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val recipientIdColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
            val dateColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.READ)

            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(idColumn)
                val recipientIds = cursor.getString(recipientIdColumn)
                    .split(" ")
                    .fastMap { it.toLongOrNull() ?: 0 }
                val date = cursor.getLong(dateColumn)
                val read = cursor.getInt(readColumn) != 0
                val rawRecipients = recipientIds.fastMap { it.getNumberForId() }
                val recipients = rawRecipients.fastMap { it.getContactNameOrNothing(context).beautifyNumber() }
                val isGroupChat = rawRecipients.size > 1
                val snippet = cursor.getString(snippetColumn) ?: getMmsThreadSnippet(threadId)

                conversations.add(
                    CuteConversation(
                        threadId = threadId,
                        rawRecipients = rawRecipients,
                        recipients = recipients,
                        snippet = snippet.trimIndent(),
                        date = date,
                        read = read,
                        isGroupChat = isGroupChat,
                        isSenderBlocked = if (isGroupChat) false else blockedNumbersManager.isNumberBlocked(rawRecipients.first()),
                    )
                )
            }
        }

       return conversations
    }

    private fun Long.getNumberForId(): String {

        val uri = Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses")

        context.contentResolver.query(
            uri,
            arrayOf(Mms.Addr.ADDRESS),
            "${Mms._ID} = ?",
            arrayOf(this.toString()),
            null
        )?.use { cursor ->
            val addressColumn = cursor.getColumnIndexOrThrow(Mms.Addr.ADDRESS)
            if (cursor.moveToFirst()) {
                val address = cursor.getString(addressColumn)
                return address
            }
        }
        return ""
    }



private fun getMmsThreadSnippet(threadId: Long): String {
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Mms.Part.CONTENT_URI
    } else {
        "content://mms/part".toUri()
    }

    val projection = arrayOf(
        Mms.Part.CONTENT_TYPE,
        Mms.Part.TEXT,
    )
    // today i learnt u can cross SQL select
    val selection = "${Mms.Part.MSG_ID} = (SELECT ${Mms._ID} FROM $MMS_TABLE_NAME WHERE ${Mms.THREAD_ID} = ? ORDER BY ${Mms.DATE} DESC LIMIT 1)"
    val selectionArgs = arrayOf(threadId.toString())

    context.contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        null,
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

        context.contentResolver.delete(Telephony.Sms.CONTENT_URI, "${Telephony.Sms.THREAD_ID} IN ($placeholders)", stringThreadIds)
        context.contentResolver.delete(Mms.CONTENT_URI, "${Mms.THREAD_ID} IN ($placeholders)", stringThreadIds)
        context.contentResolver.delete(Telephony.Threads.CONTENT_URI, "${Telephony.Threads._ID} IN ($placeholders)", stringThreadIds)
    }

    companion object {
        // extracted by logging an MMS selection query, hope it's correct and consistent across OEMs
        const val MMS_TABLE_NAME = "pdu"
    }

}