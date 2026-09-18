package com.sosauce.cinnamon.features.messaging.data

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sosauce.cinnamon.core.system.workers.SendMessageWorker
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessageEntity
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessagesDao
import com.sosauce.cinnamon.features.messaging.data.model.toCuteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ScheduledMessageManager(
    private val context: Context,
    private val dao: ScheduledMessagesDao,
    private val workManager: WorkManager
) {


    fun getScheduledMessagesForThread(threadId: Long) = dao.getScheduledMessagesForThread(threadId)
        .map { scheduledMessages ->
            scheduledMessages.fastMap { it.toCuteMessage(context) }
        }

    suspend fun schedule(message: ScheduledMessageEntity) = withContext(Dispatchers.IO) {
        val id = dao.upsertScheduledMessage(message)

        val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInitialDelay(
                message.sendAt - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                workDataOf(
                    SendMessageWorker.SCHEDULED_MESSAGE_ID to id
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            "Scheduled message ID: $id",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun delete(message: ScheduledMessageEntity) = withContext(Dispatchers.IO) {
        dao.deleteScheduledMessage(message)
        workManager.cancelUniqueWork("Scheduled message ID: ${message.id}")
    }
}