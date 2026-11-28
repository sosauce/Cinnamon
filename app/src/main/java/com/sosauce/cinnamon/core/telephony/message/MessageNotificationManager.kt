package com.sosauce.cinnamon.core.telephony.message

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.MainActivity
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.system.receivers.CopyOtpReceiver
import com.sosauce.cinnamon.core.system.receivers.MarkAsReadReceiver
import com.sosauce.cinnamon.core.system.receivers.MessageReplyReceiver
import com.sosauce.cinnamon.core.utils.CuteIntents
import com.sosauce.cinnamon.core.utils.RESULT_KEY
import com.sosauce.cinnamon.core.utils.THREAD_ID
import com.sosauce.cinnamon.core.utils.getAddressFromThreadId
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageNotificationManager(
    private val context: Context,
    private val cuteTelephonyManager: CuteTelephonyManager,
    private val scope: CoroutineScope,
    private val notificationManager: NotificationManager,
    private val numberLookup: NumberLookup,
    private val userPreferences: UserPreferences,
    private val conversationsRepository: ConversationsRepository
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

        prepareConversation(threadId)

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
                NotificationCompat.Builder(context, threadChannelId(threadId))
                    .setShortcutId(threadId.toString())
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
        val otp = extractOtp(message)

        // A hidden "x messages" notification has nothing worth appending to, start a new one
        threadIdAsNotif
            ?.takeUnless { it.notification.extras.getBoolean(EXTRA_CONTENT_HIDDEN) }
            ?.let { notification ->
            val activeStyle =
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification.notification)
                    ?.addMessage(receivedMessage)

            notificationManager.notify(
                threadId.toInt(),
                messageNotificationBuilder(threadId, activeStyle, otp)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
        } ?: notificationManager.notify(
            threadId.toInt(),
            messageNotificationBuilder(threadId, notificationStyle, otp)
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
        style: NotificationCompat.Style?,
        otp: String? = null
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, threadChannelId(threadId))
            .setContentIntent(contentIntent)
            .setShortcutId(threadId.toString())
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(style)
            .addAction(replyAction(threadId))
            .apply { otp?.let { addAction(copyOtpAction(threadId, it)) } }
    }

    private fun copyOtpAction(
        threadId: Long,
        otp: String
    ): NotificationCompat.Action {
        val copyIntent = Intent(context, CopyOtpReceiver::class.java).apply {
            putExtra(THREAD_ID, threadId)
            putExtra(CopyOtpReceiver.EXTRA_OTP, otp)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            threadId.toInt(),
            copyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.copy_code, otp),
            copyPendingIntent
        ).build()
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

    suspend fun openConversationNotificationSettings(threadId: Long) {
        prepareConversation(threadId)

        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                putExtra(Settings.EXTRA_CHANNEL_ID, INCOMING_MESSAGES_CHANNEL_ID)
                putExtra(Settings.EXTRA_CONVERSATION_ID, threadId.toString())
            } else {
                putExtra(Settings.EXTRA_CHANNEL_ID, threadChannelId(threadId))
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun removeConversations(threadIds: List<Long>) {
        threadIds.forEach { threadId ->
            notificationManager.cancel(threadId.toInt())
            notificationManager.deleteNotificationChannel(threadChannelId(threadId))
        }
        ShortcutManagerCompat.removeLongLivedShortcuts(context, threadIds.map { it.toString() })
    }

    /**
     * Creates the thread's own notification channel (a conversation channel on Android 11+)
     * and publishes its conversation shortcut, so the system can show per-chat settings
     */
    private suspend fun prepareConversation(threadId: Long) {
        val participants = withContext(Dispatchers.IO) {
            conversationsRepository.fetchParticipantsForThreadId(threadId)
        }
        val name = participants.name().ifEmpty { context.getString(R.string.unknown) }
        val channelId = threadChannelId(threadId)

        val channel = notificationManager.getNotificationChannel(channelId)
            ?: buildThreadChannel(threadId, channelId, name)
        channel.name = name
        notificationManager.createNotificationChannel(channel)

        val persons = participants.map { participant ->
            Person.Builder()
                .setName(participant.displayName)
                .setKey(participant.rawNumber)
                .setUri("tel:${participant.rawNumber}")
                .build()
        }
        val icon = participants.singleOrNull()?.let { numberLookup.fetchPersonIcon(it.photoUri) }
            ?: IconCompat.createWithResource(context, R.drawable.app_icon)
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = CuteIntents.NOTIFICATION_NAVIGATE_TO_THREAD
            putExtra("threadId", threadId)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, threadId.toString())
            .setShortLabel(name)
            .setLongLived(true)
            .setPersons(persons.toTypedArray())
            .setIcon(icon)
            .setIntent(shortcutIntent)
            .setCategories(setOf(ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    private fun buildThreadChannel(
        threadId: Long,
        channelId: String,
        name: String
    ): NotificationChannel {
        val parent = notificationManager.getNotificationChannel(INCOMING_MESSAGES_CHANNEL_ID)
        return NotificationChannel(
            channelId,
            name,
            parent?.importance ?: NotificationManager.IMPORTANCE_HIGH
        ).apply {
            group = MESSAGES_GROUP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setConversationId(INCOMING_MESSAGES_CHANNEL_ID, threadId.toString())
            }
            parent?.let {
                setSound(it.sound, it.audioAttributes)
                enableVibration(it.shouldVibrate())
                vibrationPattern = it.vibrationPattern
                enableLights(it.shouldShowLights())
                lightColor = it.lightColor
            }
        }
    }

    private fun threadChannelId(threadId: Long) = "$THREAD_CHANNEL_PREFIX$threadId"

    companion object {
        const val INCOMING_MESSAGES_CHANNEL_ID = "incoming_messages_channel"
        const val MESSAGES_GROUP = "messages_group"
        private const val THREAD_CHANNEL_PREFIX = "thread_channel_"

        /** Marks notifications posted while the app lock hides message content */
        private const val EXTRA_CONTENT_HIDDEN = "com.sosauce.cinnamon.CONTENT_HIDDEN"
    }
}

// otp detection is english only

private val otpKeyword = Regex("\\b(code|otp|verif\\w*|pass(word|code)?|pin)\\b", RegexOption.IGNORE_CASE)
private val otpCode = Regex("\\b\\d{4,8}\\b")
private fun extractOtp(message: String): String? =
    if (otpKeyword.containsMatchIn(message)) otpCode.find(message)?.value else null

/**
 * Used to prevent notifications for current threadId
 */
object ActiveThreadId {
    var threadId: Long? = null
}
