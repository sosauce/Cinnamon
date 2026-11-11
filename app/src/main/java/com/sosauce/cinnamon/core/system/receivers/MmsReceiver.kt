package com.sosauce.cinnamon.core.system.receivers

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Telephony.Mms
import android.widget.Toast
import androidx.core.net.toUri
import com.klinker.android.send_message.MmsReceivedReceiver
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.telephony.message.MessageNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MmsReceiver : MmsReceivedReceiver(), KoinComponent {
    private val messageNotificationManager by inject<MessageNotificationManager>()
    private val userPreferences by inject<UserPreferences>()
    private val scope = CoroutineScope(Job())

    override fun onMessageReceived(context: Context, uri: Uri?) {
        val mmsUri = uri ?: return
        val pending = goAsync()

        scope.launch(Dispatchers.IO) {
            try {
                val mms = context.contentResolver.query(
                    mmsUri,
                    arrayOf(Mms._ID, Mms.THREAD_ID),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) to cursor.getLong(1) else null
                } ?: return@launch
                val (mmsId, threadId) = mms

                val archiveConversations = userPreferences.archivedConversations.first()


                if (threadId.toString() in archiveConversations) return@launch

                val addressUri = Uri.withAppendedPath(
                    ContentUris.withAppendedId(Mms.CONTENT_URI, mmsId),
                    "addr"
                )

                val sender = context.contentResolver.query(
                    addressUri,
                    arrayOf(Mms.Addr.ADDRESS),
                    "${Mms.Addr.TYPE} = ?",
                    arrayOf(MMS_FROM_ADDRESS_TYPE.toString()),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }

                var imageUri: Uri? = null
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Mms.Part.CONTENT_URI
                } else {
                    "content://mms/part".toUri()
                }

                val body = context.contentResolver.query(
                    uri,
                    arrayOf(
                        Mms.Part._ID,
                        Mms.Part.CONTENT_TYPE,
                        Mms.Part.TEXT
                    ),
                    "${Mms.Part.MSG_ID} = ?",
                    arrayOf(mmsId.toString()),
                    "${Mms.Part.SEQ} ASC"
                )?.use { cursor ->
                    buildString {

                        val partIdColumn = cursor.getColumnIndexOrThrow(Mms.Part._ID)
                        val mimeTypeColumn = cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_TYPE)
                        val textColumn = cursor.getColumnIndexOrThrow(Mms.Part.TEXT)

                        while (cursor.moveToNext()) {
                            val partId = cursor.getLong(partIdColumn)
                            val mimeType = cursor.getString(mimeTypeColumn).orEmpty()

                            when {
                                mimeType == "text/plain" -> {
                                    val text = cursor.getString(textColumn).orEmpty()
                                    append(text)
                                }
                                mimeType.startsWith("image/") && imageUri == null -> {
                                    imageUri = ContentUris.withAppendedId(uri, partId)
                                }
                            }
                        }
                    }
                }.orEmpty()

                messageNotificationManager.sendOrAppendMessageNotification(
                    threadId = threadId,
                    message = body,
                    number = sender,
                    imageUri = imageUri
                )
            } finally {
                pending.finish()
            }
        }
    }

    override fun onError(context: Context, error: String?) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MMS_FROM_ADDRESS_TYPE = 137
    }
}
