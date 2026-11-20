package com.sosauce.cinnamon.core.telephony.message

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.MainActivity
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.system.receivers.MarkAsReadReceiver
import com.sosauce.cinnamon.core.system.receivers.MessageReplyReceiver
import com.sosauce.cinnamon.core.utils.CuteIntents
import com.sosauce.cinnamon.core.utils.RESULT_KEY
import com.sosauce.cinnamon.core.utils.THREAD_ID
import com.sosauce.cinnamon.core.utils.getAddressFromThreadId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MessageNotificationManager(
    private val context: Context,
    private val cuteTelephonyManager: CuteTelephonyManager,
    private val scope: CoroutineScope,
    private val notificationManager: NotificationManager,
    private val numberLookup: NumberLookup,
    private val userPreferences: UserPreferences
) {

    private val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else 0

    // TODO: make this navigate to the discussion
    private val intent = Intent(context, MainActivity::class.java)
    private val contentIntent =
        PendingIntent.getActivity(context, 1001, intent, PendingIntent.FLAG_IMMUTABLE)


    /**
     * This should only be used the reply to a message via a notification
     * @param message String message given by the remote input, NOT a CuteMessage
     */
    fun reply(
        threadId: Long,
        message: String,
        person: Person
    ) {
        // Nullable tho if we're replying to a notification obviously it should exist
        val threadIdAsNotif =
            notificationManager.activeNotifications.find { it.id == threadId.toInt() }
        val messageStyle =
            NotificationCompat.MessagingStyle.Message(message, System.currentTimeMillis(), person)

        threadIdAsNotif?.let { notification ->
            val activeStyle = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification.notification)
                ?.addMessage(messageStyle)
            notificationManager.notify(
                threadId.toInt(),
                messageNotificationBuilder(threadId, activeStyle)
                    .build()
            )
        }
        scope.launch {
            cuteTelephonyManager.sendMessage(
                addresses = listOf(threadId.getAddressFromThreadId(context)),
                message = message,
                attachments = emptyList()
            )
        }
    }

    suspend fun sendOrAppendMessageNotification(
        threadId: Long,
        message: String,
        number: String?,
        imageUri: Uri? = null
    ) {

        if (ActiveThreadId.threadId == threadId) return

        val personIcon = number?.let { numberLookup.fetchPersonIcon(it) }
        val name = number?.let { numberLookup.fetchContactDisplayName(it) ?: it }
            ?: context.getString(R.string.unknown)


        val person = Person.Builder()
            .setIcon(personIcon)
            .setName(name)
            .build()

        val threadIdAsNotif =
            notificationManager.activeNotifications.find { it.id == threadId.toInt() }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = CuteIntents.NOTIFICATION_NAVIGATE_TO_THREAD
            putExtra("threadId", threadId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            threadId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val appLockEnabled = userPreferences.appLockEnabled.first()

        // app lock disables certain things about message notif
        if (appLockEnabled) {
            val previousCount = threadIdAsNotif?.notification?.let { notification ->
                if (notification.extras.getBoolean(EXTRA_CONTENT_HIDDEN)) {
                    notification.number
                } else {
                    NotificationCompat.MessagingStyle
                        .extractMessagingStyleFromNotification(notification)
                        ?.messages?.size ?: 0
                }
            } ?: 0
            val count = previousCount + 1
            val summary = NotificationCompat.MessagingStyle.Message(
                context.resources.getQuantityString(
                    R.plurals.locked_notification_messages,
                    count,
                    count
                ),
                System.currentTimeMillis(),
                person
            )
            val contentHiddenExtra = Bundle().apply { putBoolean(EXTRA_CONTENT_HIDDEN, true) }

            notificationManager.notify(
                threadId.toInt(),
                NotificationCompat.Builder(context, INCOMING_MESSAGES_CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setStyle(NotificationCompat.MessagingStyle(person).addMessage(summary))
                    .setNumber(count)
                    .addExtras(contentHiddenExtra)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
            return
        }

        val receivedMessage =
            NotificationCompat.MessagingStyle.Message(message, System.currentTimeMillis(), person)
                .apply {
                    if (imageUri != null) {
                        setData("image/", imageUri)
                    }
                }
        val notificationStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(receivedMessage)

        // A hidden "x messages" notification has nothing worth appending to, start a new one
        threadIdAsNotif
            ?.takeUnless { it.notification.extras.getBoolean(EXTRA_CONTENT_HIDDEN) }
            ?.let { notification ->
            val activeStyle =
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification.notification)
                    ?.addMessage(receivedMessage)

            notificationManager.notify(
                threadId.toInt(),
                messageNotificationBuilder(threadId, activeStyle)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
        } ?: notificationManager.notify(
            threadId.toInt(),
            messageNotificationBuilder(threadId, notificationStyle)
                .addAction(markAsReadAction(threadId))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    /**
     * What every message notification has in common, including its reply action
     */
    private fun messageNotificationBuilder(
        threadId: Long,
        style: NotificationCompat.Style?
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, INCOMING_MESSAGES_CHANNEL_ID)
            .setContentIntent(contentIntent)
            .setChannelId(INCOMING_MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(style)
            .addAction(replyAction(threadId))
    }

    private fun replyAction(
        threadId: Long
    ): NotificationCompat.Action {
        val replyIntent = Intent(context, MessageReplyReceiver::class.java).apply {
            putExtra(THREAD_ID, threadId)
        }
        val replyPendingIntent =
            PendingIntent.getBroadcast(context, threadId.toInt(), replyIntent, flag)

        val remoteInput = RemoteInput.Builder(RESULT_KEY)
            .setLabel(context.getString(R.string.type_here))
            .build()

        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.reply),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()
    }

    private fun markAsReadAction(
        threadId: Long
    ): NotificationCompat.Action {
        val readIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            putExtra(THREAD_ID, threadId)
        }
        val readPendingIntent =
            PendingIntent.getBroadcast(context, threadId.toInt(), readIntent, flag)

        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.mark_as_read),
            readPendingIntent
        ).build()
    }

    fun clearThreadNotifications(threadId: Long) = notificationManager.cancel(threadId.toInt())

    companion object {
        const val INCOMING_MESSAGES_CHANNEL_ID = "incoming_messages_channel"
        const val MESSAGES_GROUP = "messages_group"

        /** Marks notifications posted while the app lock hides message content */
        private const val EXTRA_CONTENT_HIDDEN = "com.sosauce.cinnamon.CONTENT_HIDDEN"
    }
}

/**
 * Used to prevent notifications for current threadId
 */
object ActiveThreadId {
    var threadId: Long? = null
}
