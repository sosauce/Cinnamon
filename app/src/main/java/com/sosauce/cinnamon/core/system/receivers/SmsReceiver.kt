@file:OptIn(DelicateCoroutinesApi::class)

package com.sosauce.cinnamon.core.system.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.telephony.message.CuteTelephonyManager
import com.sosauce.cinnamon.core.telephony.message.MessageNotificationManager
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmsReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != "android.provider.Telephony.SMS_DELIVER") return

        val messagesNotificationManager by inject<MessageNotificationManager>()
        val cuteTelephonyManager by inject<CuteTelephonyManager>()
        val userPreferences by inject<UserPreferences>()
        val scope = CoroutineScope(Dispatchers.IO)
        val parts = Sms.Intents.getMessagesFromIntent(intent) ?: return
        val message = buildString { parts.forEach { append(it.messageBody.orEmpty()) } }
        val address = parts.firstOrNull()?.originatingAddress ?: return
        val pending = goAsync()

        scope.launch {
            try {
                val archived = userPreferences.archivedConversations.first()
                val threadId = address.getThreadIdOrCreate(context)
                cuteTelephonyManager.saveSmsToDevice(
                    address = address,
                    message = message,
                    messageType = Sms.MESSAGE_TYPE_INBOX,
                    read = 0
                )
                if (threadId.toString() in archived) return@launch
                messagesNotificationManager.sendOrAppendMessageNotification(
                    threadId = threadId,
                    message = message,
                    number = address
                )
            } finally {
                pending.finish()
            }
        }

    }
}
