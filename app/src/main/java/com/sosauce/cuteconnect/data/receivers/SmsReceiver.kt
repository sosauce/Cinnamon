package com.sosauce.cuteconnect.data.receivers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.provider.Telephony
import android.provider.Telephony.Sms
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.data.managers.MessageNotificationManager
import com.sosauce.cuteconnect.domain.model.CuteMessage
import com.sosauce.cuteconnect.domain.repository.CommonRepository
import com.sosauce.cuteconnect.utils.RESULT_KEY
import com.sosauce.cuteconnect.utils.getContactNameOrNothing
import com.sosauce.cuteconnect.utils.getThreadIdOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != "android.provider.Telephony.SMS_DELIVER") return

        val messagesNotificationManager by lazy { MessageNotificationManager(context) }
        val commonRepository by lazy { CommonRepository(context) }

        Sms.Intents.getMessagesFromIntent(intent).forEach { message ->

            val threadId = runBlocking(Dispatchers.IO) {
                message.displayOriginatingAddress?.getThreadIdOrCreate(context) ?: 0
            }

            val cuteMessage = CuteMessage(
                address = message.displayOriginatingAddress ?: "",
                body = message.messageBody,
                type = Sms.MESSAGE_TYPE_INBOX,
                threadId = threadId,
                read = false
            )

            commonRepository.saveSmsToDevice(cuteMessage)
            messagesNotificationManager.sendOrAppendMessageNotification(
                threadId = threadId,
                message = cuteMessage,
                number = message.displayOriginatingAddress
            )
        }
    }
}