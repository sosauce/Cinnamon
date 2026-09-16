package com.sosauce.cinnamon.core.system.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sosauce.cinnamon.core.utils.THREAD_ID
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MarkAsReadReceiver: BroadcastReceiver(), KoinComponent {



    override fun onReceive(context: Context?, intent: Intent) {

        val threadId = intent.getLongExtra(THREAD_ID, Long.MIN_VALUE)
        if (threadId == Long.MIN_VALUE) return

        val conversationsRepository by inject<ConversationsRepository>()
        val ioScope by inject<CoroutineScope>()
        val notifManager by inject<NotificationManager>()

        ioScope.launch {
            conversationsRepository.markConversationAsRead(threadId)
            notifManager.cancel(threadId.toInt())
        }

    }
}