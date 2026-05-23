package com.sosauce.cinnamon.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.managers.MessageNotificationManager
import com.sosauce.cinnamon.utils.RESULT_KEY
import com.sosauce.cinnamon.utils.THREAD_ID
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessageReplyReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val messagesNotificationManager by inject<MessageNotificationManager>()
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