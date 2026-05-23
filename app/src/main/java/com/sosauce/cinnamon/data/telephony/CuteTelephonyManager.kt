package com.sosauce.cinnamon.data.telephony

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.contentValuesOf
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.broadcasts.DeliveryReportsReceiver
import com.sosauce.cinnamon.data.datastore.UserPreferences
import com.sosauce.cinnamon.data.receivers.CuteMmsSentReceiver
import com.sosauce.cinnamon.utils.getThreadIdOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CuteTelephonyManager(
    private val context: Context,
    private val userPreferences: UserPreferences
) {


    private fun getSmsManager(subId: Int): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
        } else SmsManager.getSmsManagerForSubscriptionId(subId)
    }


    suspend fun markConversationAsRead(threadId: Long) = withContext(Dispatchers.IO) {
        val contentValues = contentValuesOf(
            Sms.READ to 1
        )
        val selection = "${Sms.THREAD_ID} = ?"

        context.contentResolver.update(
            Sms.CONTENT_URI,
            contentValues,
            selection,
            arrayOf(threadId.toString())
        )
    }


    /**
     * Send an SMS or MMS depending on which is more appropriate. Also takes into account user preferences before switching to the right protocol
     */
    suspend fun sendMessage(
        addresses: List<String>,
        message: String,
        attachments: List<Uri>
    ) {
        val isMms =
            attachments.isNotEmpty() || (addresses.size > 1 && userPreferences.groupAsMms.first()) || (message.length > 160 && userPreferences.longAsMms.first())

        if (isMms) {
            if (attachments.isNotEmpty()) {
                // one by one to limit risks of reaching carrier size limit
                attachments.fastForEachIndexed { index, attachment ->
                    sendMms(
                        addresses = addresses,
                        message = if (index == attachments.lastIndex) message else "",
                        attachment = attachment
                    )
                }
            } else {
                sendMms(
                    addresses = addresses,
                    message = message,
                    attachment = null
                )
            }
        } else {
            addresses.fastForEach { address ->
                sendSms(address, message)
            }
        }

    }

    // TODO, check if saved default sim is still active, if not, set back to default device sim
    private suspend fun sendSms(
        address: String,
        message: String
    ) = withContext(Dispatchers.IO) {
        val smsManager = getSmsManager(userPreferences.defaultMessagesSim.first())
        val messageUri = saveSmsToDevice(address, message, Sms.MESSAGE_TYPE_SENT, read = 1)

        val deliveryReportIntent = Intent(context, DeliveryReportsReceiver::class.java).apply {
            data = messageUri
        }
        val deliveryReportPending = if (userPreferences.enableDeliveryReports.first()) {
            PendingIntent.getBroadcast(
                context,
                0,
                deliveryReportIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        if (message.length <= 160) {
            smsManager.sendTextMessage(address, null, message, null, deliveryReportPending)
        } else {
            val messages = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(address, null, messages, null, null)
        }
    }

    // TODO: messageId is for resending failed mms, so move that logic elsewhere
    private suspend fun sendMms(
        addresses: List<String>,
        message: String,
        attachment: Uri?
    ) = withContext(Dispatchers.IO) {

        val settings = Settings().apply {
            useSystemSending = true
            deliveryReports = userPreferences.enableDeliveryReports.first()
            sendLongAsMms = true
            sendLongAsMmsAfter = 1
            group = true
        }
        val transaction = Transaction(context, settings)
        val message = Message(message, addresses.toTypedArray())

        if (attachment != null) {
            context.contentResolver.openInputStream(attachment)?.use {
                val bytes = it.readBytes()
                val mimeType = context.contentResolver.getType(attachment)
                val name = context.getString(R.string.unknown) // TODO get filename
                message.addMedia(bytes, mimeType, name)
            }
        }

        val mmsSentIntent = Intent(context, CuteMmsSentReceiver::class.java)
        transaction.setExplicitBroadcastForSentMms(mmsSentIntent)
        transaction.sendNewMessage(message, addresses.getThreadIdOrCreate(context))
    }

    suspend fun saveSmsToDevice(
        address: String,
        message: String,
        messageType: Int,
        read: Int
    ): Uri? = withContext(Dispatchers.IO) {

        val values = contentValuesOf(
            Sms.ADDRESS to address,
            Sms.THREAD_ID to address.getThreadIdOrCreate(context),
            Sms.DATE to System.currentTimeMillis(),
            Sms.BODY to message,
            Sms.TYPE to messageType,
            Sms.READ to read
        )

        context.contentResolver.insert(Sms.CONTENT_URI, values)
    }

}