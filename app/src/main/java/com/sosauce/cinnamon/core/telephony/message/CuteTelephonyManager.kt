package com.sosauce.cinnamon.core.telephony.message

import android.app.PendingIntent
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.Telephony
import android.telephony.SmsMessage
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
import com.sosauce.cinnamon.core.system.receivers.SmsSentReceiver
import com.sosauce.cinnamon.core.telephony.PhoneNumberNormalizer
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.core.utils.getFileName
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import androidx.core.graphics.scale
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

class CuteTelephonyManager(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {

    private val pendingIntentRequestCodes = AtomicInteger(System.currentTimeMillis().toInt())


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

        val smsPartCount = SmsMessage.calculateLength(message, false)[0]
        val isMms =
            attachments.isNotEmpty() || (normalizedAddresses.size > 1 && userPreferences.groupAsMms.first()) || (smsPartCount > 1 && userPreferences.longAsMms.first())

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
            saveSmsToDevice(address, message, Telephony.Sms.MESSAGE_TYPE_OUTBOX, read = 1)
                ?: return@withContext

        val parts = smsManager.divideMessage(message)
        val sentIntents = List(parts.size) { index ->
            val sentIntent = Intent(context, SmsSentReceiver::class.java).apply {
                data = Uri.withAppendedPath(messageUri, "sent/$index")
                putExtra(SmsSentReceiver.MESSAGE_URI, messageUri.toString())
            }
            PendingIntent.getBroadcast(
                context,
                0,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val deliveryReportPendings = if (userPreferences.enableDeliveryReports.first()) {
            parts.map {
                val deliveryReportIntent = Intent(context, DeliveryReportsReceiver::class.java).apply {
                    data = messageUri
                }
                PendingIntent.getBroadcast(
                    context,
                    pendingIntentRequestCodes.getAndIncrement(),
                    deliveryReportIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }.toCollection(ArrayList())
        } else null

        if (parts.size == 1) {
            smsManager.sendTextMessage(address, null, message, sentIntents.first(), deliveryReportPendings?.firstOrNull())
        } else {
            smsManager.sendMultipartTextMessage(
                address,
                null,
                parts,
                sentIntents.toCollection(ArrayList()),
                deliveryReportPendings
            )
        }
    }

    // TODO: messageId is for resending failed mms, so move that logic elsewhere
    private suspend fun sendMms(
        addresses: List<String>,
        message: String,
        attachment: Uri?
    ) = withContext(Dispatchers.IO) {

        val maxMessageSize = runCatching {
            getSmsManager(userPreferences.defaultMessagesSim.first())
                .carrierConfigValues
                .getInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE)
        }.getOrDefault(300_000).coerceAtLeast(100_000)

        val settings = Settings().apply {
            subscriptionId = userPreferences.defaultMessagesSim.first()
            useSystemSending = true
            deliveryReports = userPreferences.enableDeliveryReports.first()
            sendLongAsMms = true
            sendLongAsMmsAfter = 1
            group = addresses.size > 1
        }
        val transaction = Transaction(context, settings)
        val bodyText = message
        val message = Message(message, addresses.toTypedArray())

        if (attachment != null) {
            context.contentResolver.openInputStream(attachment)?.use {
                val mimeType = context.contentResolver.getType(attachment)
                val name = attachment.getFileName(context) ?: context.getString(R.string.unknown)
                // 5 kb is just a safety net here, technically not needed but better saf than sorry
                val compressionThreshold = (maxMessageSize - bodyText.toByteArray().size - 5_000).coerceAtLeast(maxMessageSize / 2)
                val imageBytes = if (mimeType?.startsWith("image/") == true) {
                    compressImage(
                        image = attachment,
                        compressionThreshold = compressionThreshold
                    )
                } else null
                val bytes = imageBytes ?: it.readBytes()

                message.addMedia(bytes, if (imageBytes != null) "image/jpeg" else mimeType, name)
            }
        }

        val mmsSentIntent = Intent(context, CuteMmsSentReceiver::class.java)
        transaction.setExplicitBroadcastForSentMms(mmsSentIntent)
        //transaction.sendNewMessage(message, addresses.getThreadIdOrCreate(context))
        transaction.sendNewMessage(message)
    }

    suspend fun compressImage(
        image: Uri,
        compressionThreshold: Int
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            val inputBytes = context
                .contentResolver
                .openInputStream(image)
                ?.use { inputStream ->
                    inputStream.readBytes()
                } ?: return@withContext null

            if (inputBytes.size < compressionThreshold) return@withContext inputBytes
            val mimeType = context.contentResolver.getType(image)

            ensureActive()

            withContext(Dispatchers.Default) {
                val bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)

                ensureActive()

                val compressFormat = when(mimeType) {
                    "image/png" -> Bitmap.CompressFormat.PNG
                    "image/jpeg" -> Bitmap.CompressFormat.JPEG
                    "image/webp" -> if(Build.VERSION.SDK_INT >= 30) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }

                var outputBytes: ByteArray
                var quality = 90

                do {
                    ByteArrayOutputStream().use { outputStream ->
                        bitmap.compress(compressFormat, quality, outputStream)
                        outputBytes = outputStream.toByteArray()
                        quality -= (quality * 0.1).roundToInt()
                    }
                } while(isActive &&
                    outputBytes.size > compressionThreshold &&
                    quality > 5 &&
                    compressFormat != Bitmap.CompressFormat.PNG
                )

                outputBytes
            }
        }
    }

    suspend fun saveSmsToDevice(
        address: String,
        message: String,
        messageType: Int,
        read: Int,
        subscriptionId: Int? = null
    ): Uri? = withContext(Dispatchers.IO) {

        val values = contentValuesOf(
            Telephony.Sms.ADDRESS to address,
            Telephony.Sms.THREAD_ID to address.getThreadIdOrCreate(context),
            Telephony.Sms.DATE to System.currentTimeMillis(),
            Telephony.Sms.BODY to message,
            Telephony.Sms.TYPE to messageType,
            Telephony.Sms.READ to read
        ).apply {
            if (subscriptionId != null) {
                put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
            }
        }

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
