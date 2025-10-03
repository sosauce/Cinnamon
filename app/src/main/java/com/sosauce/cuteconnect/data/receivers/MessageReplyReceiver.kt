package com.sosauce.cuteconnect.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.data.managers.MessageNotificationManager
import com.sosauce.cuteconnect.utils.RESULT_KEY
import com.sosauce.cuteconnect.utils.THREAD_ID

class MessageReplyReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messagesNotificationManager by lazy { MessageNotificationManager(context) }
        val threadId = intent.getLongExtra(THREAD_ID, -1L)
        if (threadId == -1L) return

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val input = remoteInput?.getCharSequence(RESULT_KEY).toString()
        val person = Person.Builder()
            .setName(context.getString(R.string.me))
            .build()

        messagesNotificationManager.reply(
            threadId = threadId,
            message = input,
            person = person
        )
    }
}