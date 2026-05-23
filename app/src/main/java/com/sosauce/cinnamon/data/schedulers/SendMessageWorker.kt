package com.sosauce.cinnamon.data.schedulers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sosauce.cinnamon.data.schedulers.scheduled_messages.ScheduledMessagesDao
import com.sosauce.cinnamon.data.telephony.CuteTelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SendMessageWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KoinComponent {

    private val cuteTelephonyManager by inject<CuteTelephonyManager>()
    private val scheduledMessagesDao by inject<ScheduledMessagesDao>()

    override suspend fun doWork(): Result {

        return withContext(Dispatchers.IO) {
            return@withContext try {
                val scheduledMessageId =
                    workerParameters.inputData.getLong(SCHEDULED_MESSAGE_ID, -1L)
                if (scheduledMessageId == -1L) return@withContext Result.failure(
                    workDataOf(MESSAGE_NOT_SENT to "Scheduled message doesn't exist")
                )

                val scheduledMessage =
                    scheduledMessagesDao.getScheduledMessageById(scheduledMessageId)

                cuteTelephonyManager.sendMessage(
                    addresses = listOf(scheduledMessage.address),
                    message = scheduledMessage.message,
                    attachments = emptyList()
                )

                scheduledMessagesDao.deleteScheduledMessage(scheduledMessage)
                Result.success()
            } catch (e: Exception) {
                Result.failure(
                    workDataOf(MESSAGE_NOT_SENT to (e.message ?: "Unknown error"))
                )
            }
        }
    }

    companion object {
        const val SCHEDULED_MESSAGE_ID = "SCHEDULED_MESSAGE_ID"
        const val MESSAGE_NOT_SENT = "MESSAGE_NOT_SENT"
    }
}