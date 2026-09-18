package com.sosauce.cinnamon.core.telephony.message

import android.app.PendingIntent
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.core.content.contentValuesOf
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.system.receivers.CuteMmsSentReceiver
import com.sosauce.cinnamon.core.system.receivers.DeliveryReportsReceiver
import com.sosauce.cinnamon.core.telephony.PhoneNumberNormalizer
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CuteTelephonyManager(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {


    private fun getSmsManager(subId: Int): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
        } else SmsManager.getSmsManagerForSubscriptionId(subId)
    }


    /**
     * Send an SMS or MMS depending on which is more appropriate. Also takes into account user preferences before switching to the right protocol
     */
    suspend fun sendMessage(
        addresses: List<String>,
        message: String,
        attachments: List<Uri>
    ) {


        val normalizedAddresses = addresses.fastMap {
            phoneNumberNormalizer.formatToE164(it)
        }

        val isMms =
            attachments.isNotEmpty() || (normalizedAddresses.size > 1 && userPreferences.groupAsMms.first()) || (message.length > 160 && userPreferences.longAsMms.first())

        if (isMms) {
            if (attachments.isNotEmpty()) {
                // one by one to limit risks of reaching carrier size limit
                attachments.fastForEachIndexed { index, attachment ->
                    sendMms(
                        addresses = normalizedAddresses,
                        message = if (index == attachments.lastIndex) message else "",
                        attachment = attachment
                    )
                }
            } else {
                sendMms(
                    addresses = normalizedAddresses,
                    message = message,
                    attachment = null
                )
            }
        } else {
            normalizedAddresses.fastForEach { address ->
                sendSms(address, message)
            }
        }

    }

    // TODO, check if saved default sim is still active, if not, set back to default device sim?
    private suspend fun sendSms(
        address: String,
        message: String
    ) = withContext(Dispatchers.IO) {
        val smsManager = getSmsManager(userPreferences.defaultMessagesSim.first())
        val messageUri =
            saveSmsToDevice(address, message, Telephony.Sms.MESSAGE_TYPE_SENT, read = 1)

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
            Telephony.Sms.ADDRESS to address,
            Telephony.Sms.THREAD_ID to address.getThreadIdOrCreate(context),
            Telephony.Sms.DATE to System.currentTimeMillis(),
            Telephony.Sms.BODY to message,
            Telephony.Sms.TYPE to messageType,
            Telephony.Sms.READ to read
        )

        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
    }

    /**
     * It can also take emails
     * @return Whether blocking was successful
     */
    suspend fun blockNumbers(numbers: List<String>): Boolean = withContext(Dispatchers.IO) {
        val ops = ArrayList<ContentProviderOperation>()

        numbers.fastForEach { number ->
            ops.add(
                ContentProviderOperation.newInsert(BlockedNumbers.CONTENT_URI)
                    .withValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                    .build()
            )
        }
        return@withContext try {
            context.contentResolver.applyBatch(BlockedNumberContract.AUTHORITY, ops)
            true
        } catch (_: Exception) {
            false
        }
    }

}