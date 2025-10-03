package com.sosauce.cuteconnect.domain.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.MediaStore
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.MmsSms
import android.provider.Telephony.Sms
import android.provider.VoicemailContract
import android.speech.tts.Voice
import android.telephony.CarrierConfigManager
import android.telephony.PhoneNumberUtils
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.SparseArray
import android.util.Xml
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.util.fastForEach
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import androidx.room.Room
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingsDao
import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingsDatabase
import com.sosauce.cuteconnect.domain.model.CuteAttachment
import com.sosauce.cuteconnect.domain.model.CuteCallLog
import com.sosauce.cuteconnect.domain.model.CuteContact
import com.sosauce.cuteconnect.domain.model.CuteConversation
import com.sosauce.cuteconnect.domain.model.CuteMessage
import com.sosauce.cuteconnect.domain.model.CuteSimCard
import com.sosauce.cuteconnect.domain.model.CuteVoicemail
import com.sosauce.cuteconnect.utils.PermissionUtils
import com.sosauce.cuteconnect.utils.copyMutate
import com.sosauce.cuteconnect.utils.getThreadIdOrCreate
import com.sosauce.cuteconnect.utils.observe
import com.sosauce.cuteconnect.utils.observeSims
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Locale
import kotlin.toString

class CommonRepository(
    private val context: Context
) {

    private val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
    } else SmsManager.getDefault()

    private val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)






//    fun fetchLatestMessages(): Flow<List<CuteMessage>> {
//        return context.contentResolver.observe(Sms.CONTENT_URI).map {
//            fetchMessages()
//        }.flowOn(Dispatchers.IO)
//    }

    fun fetchLatestMessagesForThread(threadId: Long): Flow<List<CuteMessage>> {
        return context.contentResolver.observe(Sms.CONTENT_URI).map {
            fetchMessagesForThread(threadId)
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestConversations(): Flow<List<CuteConversation>> {
        return context.contentResolver.observe(Telephony.Threads.CONTENT_URI).map {
            fetchConversations()
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestContacts(): Flow<List<CuteContact>> {
        return context.contentResolver.observe(ContactsContract.Data.CONTENT_URI).map {
            fetchContacts()
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestCallLog(): Flow<List<CuteCallLog>> {
        return context.contentResolver.observe(CallLog.Calls.CONTENT_URI).map {
            fetchCallLogs()
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestVoicemails(): Flow<List<CuteVoicemail>> {
        return context.contentResolver.observe(VoicemailContract.Voicemails.CONTENT_URI).map {
            fetchVoicemails()
        }.flowOn(Dispatchers.IO)
    }

    fun fetchLatestSims(): Flow<List<CuteSimCard>> {
        return subscriptionManager.observeSims(context).map {
            fetchSims()
        }
    }




    private fun fetchMessages(): List<CuteMessage> {

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
        )

        context.contentResolver.query(
            Sms.CONTENT_URI,
            projection,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Sms._ID)
            val threadIdColumn = cursor.getColumnIndexOrThrow(Sms.THREAD_ID)
            val addressColumn = cursor.getColumnIndexOrThrow(Sms.ADDRESS)
            val dateColumn = cursor.getColumnIndexOrThrow(Sms.DATE)
            val bodyColumn = cursor.getColumnIndexOrThrow(Sms.BODY)
            val typeColumn = cursor.getColumnIndexOrThrow(Sms.TYPE)
            val readColumn = cursor.getColumnIndexOrThrow(Sms.READ)


            while (cursor.moveToNext()) {
                messages.add(
                    CuteMessage(
                        id = cursor.getLong(idColumn),
                        body = cursor.getString(bodyColumn),
                        type = cursor.getInt(typeColumn),
                        threadId = cursor.getLong(threadIdColumn),
                        address = cursor.getString(addressColumn),
                        date = cursor.getLong(dateColumn),
                        read = cursor.getInt(readColumn) == 1
                    )
                )
            }

            messages.addAll(fetchMms())
        }
        return messages
    }

    private fun fetchMessagesForThread(threadId: Long): List<CuteMessage> {

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
        )

        val selection = "${Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        context.contentResolver.query(
            Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Sms._ID)
            val threadIdColumn = cursor.getColumnIndexOrThrow(Sms.THREAD_ID)
            val addressColumn = cursor.getColumnIndexOrThrow(Sms.ADDRESS)
            val dateColumn = cursor.getColumnIndexOrThrow(Sms.DATE)
            val bodyColumn = cursor.getColumnIndexOrThrow(Sms.BODY)
            val typeColumn = cursor.getColumnIndexOrThrow(Sms.TYPE)
            val readColumn = cursor.getColumnIndexOrThrow(Sms.READ)


            while (cursor.moveToNext()) {
                messages.add(
                    CuteMessage(
                        id = cursor.getLong(idColumn),
                        body = cursor.getString(bodyColumn),
                        type = cursor.getInt(typeColumn),
                        threadId = cursor.getLong(threadIdColumn),
                        address = cursor.getString(addressColumn),
                        date = cursor.getLong(dateColumn),
                        read = cursor.getInt(readColumn) == 1
                    )
                )
            }

            messages.addAll(fetchThreadMms(threadId))
        }
        return messages.sortedBy { it.date }
    }

    private fun fetchThreadMms(threadId: Long): List<CuteMessage> {
        val mms = mutableListOf<CuteMessage>()

        val projection = arrayOf(
            Mms._ID,
            Mms.THREAD_ID,
            Mms.MESSAGE_BOX,
            Mms.READ,
            Mms.DATE
        )

        val selection = "${Mms.THREAD_ID} = ?"

        val selectionArgs = arrayOf(threadId.toString())


        context.contentResolver.query(
            Mms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Mms._ID)
            val threadIdColumn = cursor.getColumnIndexOrThrow(Mms.THREAD_ID)
            val typeColumn = cursor.getColumnIndexOrThrow(Mms.MESSAGE_BOX)
            val dateColumn = cursor.getColumnIndexOrThrow(Mms.DATE)
            val readColumn = cursor.getColumnIndexOrThrow(Mms.READ)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val threadId = cursor.getLong(threadIdColumn)
                val type = cursor.getInt(typeColumn)
                val rawDate = cursor.getLong(dateColumn)
                val date = if (rawDate > 1_000L) rawDate * 1000 else System.currentTimeMillis()
                val attachment = getMmsAttachment(id)
                val read = cursor.getInt(readColumn) == 1

                mms.add(
                    CuteMessage(
                        id = id,
                        body = attachment.body,
                        type = type,
                        threadId = threadId,
                        address = "", // TODO() get address
                        date = date,
                        read = read,
                        attachment = attachment,
                        isMms = true
                    )
                )
            }
        }
        return mms
    }



    private fun fetchMms(threadId: Long = -1): List<CuteMessage> {
        val mms = mutableListOf<CuteMessage>()

        val projection = arrayOf(
            Mms._ID,
            Mms.THREAD_ID,
            Mms.MESSAGE_BOX,
            Mms.READ,
            Mms.DATE
        )

        val selection = if (threadId != -1L) {
            "${Mms.THREAD_ID} = ?"
        } else null

        val selectionArgs = if (threadId != -1L) {
            arrayOf(threadId.toString())
        } else null

        val sortOrder = if (threadId != -1L) {
            "${Mms.DATE} DESC LIMIT 1"
        } else null

        context.contentResolver.query(
            Mms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Mms._ID)
            val threadIdColumn = cursor.getColumnIndexOrThrow(Mms.THREAD_ID)
            val typeColumn = cursor.getColumnIndexOrThrow(Mms.MESSAGE_BOX)
            val dateColumn = cursor.getColumnIndexOrThrow(Mms.DATE)
            val readColumn = cursor.getColumnIndexOrThrow(Mms.READ)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val threadId = cursor.getLong(threadIdColumn)
                val type = cursor.getInt(typeColumn)
                val rawDate = cursor.getLong(dateColumn)
                val date = if (rawDate > 1_000L) rawDate * 1000 else System.currentTimeMillis()
                val attachment = getMmsAttachment(id)
                val read = cursor.getInt(readColumn) == 1

                mms.add(
                    CuteMessage(
                        id = id,
                        body = attachment.body,
                        type = type,
                        threadId = threadId,
                        address = "", // TODO() get address
                        date = date,
                        read = read,
                        attachment = attachment,
                        isMms = true
                    )
                )
            }
        }
        return mms
    }

    /**
     * Gets a creates a conversation for a phone number
     */
    fun getOrCreateConversation(number: String): CuteConversation {

        val threadId = number.getThreadIdOrCreate(context)


        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.READ,
            Telephony.Threads.RECIPIENT_IDS
        )

        val selection = "${Telephony.Threads._ID} = ?"

        context.contentResolver.query(
            "${Telephony.Threads.CONTENT_URI}?simple=true".toUri(),
            projection,
            selection,
            arrayOf(threadId.toString()),
            "${Telephony.Threads.DATE} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val recipientIdsColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
            val dateColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.READ)


            while (cursor.moveToNext()) {

                val threadId = cursor.getLong(idColumn)
                val recipientIds = cursor.getString(recipientIdsColumn)
                val recipientIdsAsLongs = recipientIds.split(" ").map { it.toLongOrNull() ?: 0 }
                val recipientsPhoneNumber = getListOfAddresses(recipientIdsAsLongs)
                val snippet = (cursor.getString(snippetColumn) ?: "").ifEmpty { getMmsThreadSnippet(threadId) }
                val date = cursor.getLong(dateColumn)
                val read = cursor.getInt(readColumn)
                val isGroupChat = recipientsPhoneNumber.size > 1
                val contactsId = recipientsPhoneNumber.map { getContactIdForThread(it) }
                val threadContact = if (!isGroupChat) { // TODO: allow this for group chats
                    fetchContacts(
                        selection = "${ContactsContract.Data.CONTACT_ID} = ?",
                        selectionArgs = arrayOf(contactsId.first().toString())
                    )
                } else emptyList()

                return CuteConversation(
                    threadId = threadId,
                    snippet = snippet,
                    recipients = recipientsPhoneNumber,
                    contacts = threadContact,
                    isSenderBlocked = if (recipientsPhoneNumber.size > 1) false else BlockedNumbersManager.isNumberBlocked(recipientsPhoneNumber.first(), context), // TODO: Checked if anyone in the group chat is blocked
                    date = date,
                    read = read == 1,
                    isGroupChat = isGroupChat
                )
            }
        }

        return CuteConversation(
            threadId = threadId,
            snippet = "",
            recipients = listOf(number),
            contacts = emptyList(),
            isSenderBlocked = false,
            date = System.currentTimeMillis(),
            read = true,
            isGroupChat = false
        )
    }

    private fun fetchConversations(
        onlyPinned: Boolean = false
    ): List<CuteConversation> {
        val conversations = mutableListOf<CuteConversation>()

        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.READ,
            Telephony.Threads.RECIPIENT_IDS
        )



        val selection = "${Telephony.Threads.MESSAGE_COUNT} > ?"

        context.contentResolver.query(
            "${Telephony.Threads.CONTENT_URI}?simple=true".toUri(),
            projection,
            selection,
            arrayOf("0"),
            "${Telephony.Threads.DATE} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val recipientIdsColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
            val dateColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readColumn = cursor.getColumnIndexOrThrow(Telephony.Threads.READ)


            while (cursor.moveToNext()) {

                val threadId = cursor.getLong(idColumn)
                val recipientIds = cursor.getString(recipientIdsColumn)
                val recipientIdsAsLongs = recipientIds.split(" ").map { it.toLongOrNull() ?: 0 }
                val recipientsPhoneNumber = getListOfAddresses(recipientIdsAsLongs)
                val snippet = (cursor.getString(snippetColumn) ?: "").ifEmpty { getMmsThreadSnippet(threadId) }
                val date = cursor.getLong(dateColumn)
                val read = cursor.getInt(readColumn)
                val isGroupChat = recipientsPhoneNumber.size > 1
                val contactsId = recipientsPhoneNumber.map { getContactIdForThread(it) }
                val threadContact = if (!isGroupChat) { // TODO: allow this for GC
                    fetchContacts(
                        selection = "${ContactsContract.Data.CONTACT_ID} = ?",
                        selectionArgs = arrayOf(contactsId.first().toString())
                    )
                } else emptyList()

                conversations.add(
                    CuteConversation(
                        threadId = threadId,
                        snippet = snippet,
                        recipients = recipientsPhoneNumber,
                        contacts = threadContact,
                        isSenderBlocked = if (recipientsPhoneNumber.size > 1) false else BlockedNumbersManager.isNumberBlocked(recipientsPhoneNumber.first(), context), // TODO: Checked if anyone in the group chat is blocked
                        date = date,
                        read = read == 1,
                        isGroupChat = isGroupChat
                    )
                )
            }
        }
        return conversations
    }

    private fun fetchCallLogs(): List<CuteCallLog> {

        val callLogs = mutableListOf<CuteCallLog>()


        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE, // Incoming, outgoing, missed
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
        )

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val callTypeColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)


            while (cursor.moveToNext()) {
                callLogs.add(
                    CuteCallLog(
                        id = cursor.getLong(idColumn),
                        number = cursor.getString(numberColumn),
                        callType = cursor.getInt(callTypeColumn),
                        date = cursor.getLong(dateColumn),
                        duration = cursor.getLong(durationColumn)
                    )
                )
            }
        }

        return callLogs
    }
    private fun fetchVoicemails(): List<CuteVoicemail> {

        val voicemails = mutableListOf<CuteVoicemail>()

        val projection = arrayOf(
            VoicemailContract.Voicemails._ID,
            VoicemailContract.Voicemails.NUMBER,
            VoicemailContract.Voicemails.DURATION,
            VoicemailContract.Voicemails.DATE,
            // VoicemailContract.Voicemails.HAS_CONTENT, // Do we need that to assume voicemail has audio ?
        )

        context.contentResolver.query(
            VoicemailContract.Voicemails.CONTENT_URI,
            projection,
            null,
            null,
            null // TODO: sort by most recent
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails._ID)
            val numberColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails.NUMBER)
            val durationColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails.DURATION)
            val dateColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails.DATE)


            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val number = cursor.getString(numberColumn)
                val uri = ContentUris.withAppendedId(VoicemailContract.Voicemails.CONTENT_URI, id)
                val date = cursor.getLong(dateColumn)
                val duration = cursor.getLong(durationColumn)


                voicemails.add(
                    CuteVoicemail(
                        id = id,
                        address = number,
                        uri = uri,
                        duration = duration,
                        date = date
                    )
                )

            }
        }

        return voicemails

    }

    @SuppressLint("MissingPermission")
    fun fetchSims(): List<CuteSimCard> {
        val simCards = mutableListOf<CuteSimCard>()

        subscriptionManager.activeSubscriptionInfoList?.forEach { subInfo ->
            simCards.add(
                CuteSimCard(
                    subId = subInfo.subscriptionId,
                    name = subInfo.displayName?.toString() ?: "No name",
                    carrierName = subInfo.carrierName?.toString() ?: "No carrier",
                    color = subInfo.iconTint
                )
            )
        }
        return simCards
    }


    private fun fetchContacts(
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): List<CuteContact> {


        val contactsMap = mutableMapOf<Long, CuteContact>()


        // This can be a bit confusing, but it's so much faster than having individual queries for each data, no more ANR and super fast loading ^_^
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.IS_PRIMARY,
        )

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.Data.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)
            val starredColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.STARRED)
            val data1Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val mimeColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data2Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val isPrimaryColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY)

            while (cursor.moveToNext()) {


                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val isFavorite = cursor.getInt(starredColumn) != 0
                val mimeType = cursor.getString(mimeColumn)
                val data1 = cursor.getString(data1Column)
                val data2 = cursor.getInt(data2Column)
                val isPrimary = cursor.getInt(isPrimaryColumn) != 0


                val oldContact = contactsMap[id] ?: CuteContact(
                    id = id,
                    name = name,
                    photo = getContactPhoto(id),
                    phoneNumbers = emptyList(),
                    emails = emptyList(),
                    addresses = emptyList(),
                    websites = emptyList(),
                    notes = emptyList(),
                    events = emptyList(),
                    isFavorite = isFavorite,
                )

                val newContact = if (data1.isNullOrBlank()) {
                    oldContact
                } else {
                    when (mimeType) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ->
                            oldContact.copy(phoneNumbers = oldContact.phoneNumbers + CuteContact.Phone(data1, data2, isPrimary))
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ->
                            oldContact.copy(emails = oldContact.emails + CuteContact.Email(data1, data2, isPrimary))
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE ->
                            oldContact.copy(addresses = oldContact.addresses + CuteContact.Address(data1, data2, isPrimary))
                        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE ->
                            oldContact.copy(websites = oldContact.websites + CuteContact.Website(data1))
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE ->
                            oldContact.copy(notes = oldContact.notes + CuteContact.Note(data1))
                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE ->
                            oldContact.copy(events = oldContact.events + CuteContact.Event(data1, data2))
                        else -> oldContact
                    }
                }


                contactsMap[id] = newContact
            }
        }
        return contactsMap.values.toList()
    }

    private fun getListOfAddresses(ids: List<Long>): List<String> {
        val list = mutableListOf<String>()
        ids.fastForEach {
            list.add(getPhoneNumberOfRecipientId(it))
        }
        return list
    }

    /**
     * Retrieves a contact's ID from it's address, or -1 if it doesn't exist. This is later used to get a contact by ID
     */
    private fun getContactIdForThread(address: String): Long {

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            address
        )

        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)
            if (cursor.moveToFirst()) {
                return cursor.getLong(idColumn)
            }
        }

        return -1
    }


    private fun fetchLatestMmsId(threadId: Long): Int {

        context.contentResolver.query(
            Mms.CONTENT_URI,
            arrayOf(Mms._ID),
            "${Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Mms.DATE} DESC LIMIT 1"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Mms._ID)

            while (cursor.moveToFirst()) {
                return cursor.getInt(idColumn)
            }
        }
        return 0
    }
    private fun getMmsThreadSnippet(threadId: Long): String {
        val latestMmsId = fetchLatestMmsId(threadId)
        var snippet = ""


        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Mms.Part.CONTENT_URI
        } else {
            "content://mms/part".toUri()
        }

        val projection = arrayOf(
            Mms._ID,
            Mms.Part.CONTENT_TYPE,
            Mms.Part.TEXT
        )
        val selection = "${Mms.Part.MSG_ID} = ?"
        val selectionArgs = arrayOf(latestMmsId.toString())

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->

            while (cursor.moveToFirst()) {
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_TYPE))

                when {
                    mimeType == "text/plain" -> {
                        val bodyText = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.TEXT))
                        snippet = bodyText
                        break
                    }
                    mimeType.startsWith("image/") || mimeType.startsWith("video/") -> {
                        snippet = if (mimeType.startsWith("image/")) "Image" else "Video"
                        break
                    }
                    else -> {
                        snippet = "File"
                        break
                    }
                }

            }

        }
        return snippet
    }

    // Personal note: MMS also represent group chats in the SMS (probably not RCS) system
    private fun getPhoneNumberOfRecipientId(id: Long): String {
        val uri = Uri.withAppendedPath(MmsSms.CONTENT_URI, "canonical-addresses")
        val projection = arrayOf(
            Mms.Addr.ADDRESS
        )

        val selection = "${Mms._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val addressColumn = cursor.getColumnIndexOrThrow(Mms.Addr.ADDRESS)
            if (cursor.moveToFirst()) {
                return cursor.getString(addressColumn)
            }
        }
        return ""
    }

    // Since Coil takes care of loading the uri, we're fine passing the high res image
    private fun getContactPhoto(contactId: Long): Uri {
        val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId.toLong())
        val photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.DISPLAY_PHOTO)

        val hasPhoto = try {
            context.contentResolver.openInputStream(photoUri)?.use {
                it.available() > 0
            } == true
        } catch (e: Exception) {
            false
        }
        return if (hasPhoto) {
            photoUri
        } else Uri.EMPTY
    }


    // Inspired by https://github.com/FossifyOrg/Messages/blob/8c5bb9a32c990773259b4d95d698b83d31939171/app/src/main/kotlin/org/fossify/messages/extensions/Context.kt#L477
    private fun getMmsAttachment(
        messageId: Long,
    ): CuteAttachment {
        var attachment = CuteAttachment(messageId)

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Mms.Part.CONTENT_URI
        } else {
            "content://mms/part".toUri()
        }

        val projection = arrayOf(
            Mms.Part._ID,
            Mms.Part.CONTENT_TYPE,
            Mms.Part.TEXT
        )
        val selection = "${Mms.Part.MSG_ID} = ?"
        val selectionArgs = arrayOf(messageId.toString())

        var attachmentNames: List<String>? = null
        var attachmentCount = 0

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->

            while (cursor.moveToNext()) {
                val partId = cursor.getLong(cursor.getColumnIndexOrThrow(Mms._ID)) // Id meant to get the data uri of MMS if any
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_TYPE))

                if (mimeType == "text/plain") {
                    val bodyText = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.TEXT))
                    attachment = attachment.copy(
                        body = bodyText
                    )
                } else if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                    val fileUri = Uri.withAppendedPath(uri, partId.toString())
                    val attachmentDetail = CuteAttachment.AttachmentDetails(
                        id = partId,
                        uri = fileUri,
                        filename = ""
                    )

                    attachment = attachment.copy(
                        attachmentDetails = attachment.attachmentDetails.copyMutate { add(attachmentDetail) },
                    )
                } else if (mimeType != "application/smil") {
                    val fileUri = Uri.withAppendedPath(uri, partId.toString())
                    val attachmentName = attachmentNames?.getOrNull(attachmentCount) ?: ""

                    val attachmentDetail = CuteAttachment.AttachmentDetails(
                        id = partId,
                        uri = fileUri,
                        filename = attachmentName
                    )

                    attachment = attachment.copy(
                        attachmentDetails = attachment.attachmentDetails.copyMutate { add(attachmentDetail) }
                    )
                    attachmentCount++
                } else {
                    val text = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.TEXT))
                    attachmentNames = parseAttachmentNames(text)

                }
            }
        }
        return attachment
    }

    private fun parseAttachmentNames(text: String): List<String> {
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



    fun saveSmsToDevice(
        cuteMessage: CuteMessage
    ) {

        val values = contentValuesOf(
            Sms.ADDRESS to cuteMessage.address,
            Sms.THREAD_ID to cuteMessage.threadId,
            Sms.DATE to System.currentTimeMillis(),
            Sms.BODY to cuteMessage.body,
            Sms.TYPE to cuteMessage.type,
            Sms.READ to cuteMessage.read
        )

        context.contentResolver.insert(Sms.CONTENT_URI, values)
    }

    fun deleteFromContentUri(
        contentUri: Uri,
        id: Long
    ) {
        val uri = ContentUris.withAppendedId(contentUri, id)
        context.contentResolver.delete(uri, null, null)
    }

    fun sendMessage(
        address: String,
        message: String
    ) {
        smsManager.sendTextMessage(address, null, message, null, null)
    }

    fun sendMms(
        cuteAttachment: CuteAttachment
    ) {

        val settings = Settings().apply {
            useSystemSending = true
        }
        val transaction = Transaction(context)
        val message = Message()
    }

    fun markSmsAsRead(
        messageId: Long
    ) {
        val contentValues = contentValuesOf(
            Sms.READ to 1
        )
        val selectionArgs = "${Sms._ID} = ?"

        context.contentResolver.update(Sms.CONTENT_URI, contentValues, selectionArgs, arrayOf(messageId.toString()))
    }


    val conversations = fetchConversations()

}