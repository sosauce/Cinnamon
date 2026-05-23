package com.sosauce.cinnamon.data.managers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff.Mode
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.fetchers.RecipientPhone
import com.sosauce.cinnamon.data.fetchers.RecipientPhotoFetcher
import com.sosauce.cinnamon.data.receivers.MessageReplyReceiver
import com.sosauce.cinnamon.data.telephony.CuteTelephonyManager
import com.sosauce.cinnamon.main.MainActivity
import com.sosauce.cinnamon.utils.CuteIntents
import com.sosauce.cinnamon.utils.RESULT_KEY
import com.sosauce.cinnamon.utils.THREAD_ID
import com.sosauce.cinnamon.utils.getAddressFromThreadId
import com.sosauce.cinnamon.utils.getContactNameOrNothing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.min

class MessageNotificationManager(
    private val context: Context,
    private val cuteTelephonyManager: CuteTelephonyManager,
    private val scope: CoroutineScope
) {

    private val notificationManager =
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
                NotificationCompat.Builder(context, INCOMING_MESSAGES_CHANNEL_ID)
                    .setContentIntent(contentIntent)
                    .setChannelId(INCOMING_MESSAGES_CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setStyle(activeStyle)
                    .addAction(replyAction(threadId))
                    .build()
            )
            scope.launch {
                cuteTelephonyManager.sendMessage(
                    addresses = listOf(threadId.getAddressFromThreadId(context)),
                    message = message,
                    attachments = emptyList()
                )
            }
        }
    }


    // https://www.geeksforgeeks.org/kotlin/circular-crop-an-image-and-save-it-to-the-file-in-android/
    private fun circleBitmap(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val squaredBitmap = Bitmap.createBitmap(bitmap, 0, 0, size, size)

        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val paint = Paint()
        val rect = Rect(0, 0, size, size)
        val radius = (size / 2).toFloat()

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)

        canvas.drawCircle(radius, radius, radius, paint)

        paint.xfermode = PorterDuffXfermode(Mode.SRC_IN)

        canvas.drawBitmap(squaredBitmap, rect, rect, paint)

        bitmap.recycle()
        squaredBitmap.recycle()

        return output

    }

    fun sendOrAppendMessageNotification(
        threadId: Long,
        message: String,
        number: String?,
        imageUri: Uri? = null
    ) {

        if (ActiveThreadId.threadId == threadId) return

        val request = ImageRequest.Builder(context)
            .data(RecipientPhone(number ?: ""))
            .build()
        val bitmap = runBlocking(Dispatchers.IO) {
            ImageLoader.Builder(context)
                .components { add(RecipientPhotoFetcher.Factory()) }
                .build()
                .execute(request)
                .image?.toBitmap()
        }

        val personIcon = bitmap?.let { IconCompat.createWithBitmap(circleBitmap(it)) }

        val person = Person.Builder()
            .setIcon(personIcon)
            .setName(
                number?.getContactNameOrNothing(context) ?: context.getString(R.string.unknown)
            )
            .build()
        val receivedMessage =
            NotificationCompat.MessagingStyle.Message(message, System.currentTimeMillis(), person)
                .apply {
                    if (imageUri != null) {
                        setData("image/", imageUri)
                    }
                }
        val notificationStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(receivedMessage)

        val threadIdAsNotif =
            notificationManager.activeNotifications.find { it.id == threadId.toInt() }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = CuteIntents.NOTIFICATION_NAVIGATE_TO_THREAD
            putExtra("threadId", threadId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        threadIdAsNotif?.let { notification ->
            val activeStyle =
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification.notification)
                    ?.addMessage(receivedMessage)

            notificationManager.notify(
                threadId.toInt(),
                NotificationCompat.Builder(context, INCOMING_MESSAGES_CHANNEL_ID)
                    .setContentIntent(contentIntent)
                    .setChannelId(INCOMING_MESSAGES_CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setStyle(activeStyle)
                    .addAction(replyAction(threadId))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
        } ?: notificationManager.notify(
            threadId.toInt(),
            NotificationCompat.Builder(context, INCOMING_MESSAGES_CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setChannelId(INCOMING_MESSAGES_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(notificationStyle)
                .addAction(replyAction(threadId))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun replyAction(
        threadId: Long
    ): NotificationCompat.Action {
        val replyIntent = Intent(context, MessageReplyReceiver::class.java).apply {
            putExtra(THREAD_ID, threadId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(context, 1, replyIntent, flag)

        val remoteInput = RemoteInput.Builder(RESULT_KEY)
            .setLabel(context.getString(R.string.type_here))
            .build()

        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.reply),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()
    }

    fun clearThreadNotifications(threadId: Long) = notificationManager.cancel(threadId.toInt())

    companion object {
        const val INCOMING_MESSAGES_CHANNEL_ID = "incoming_messages_channel"
        const val MESSAGES_GROUP = "messages_group"
    }
}

/**
 * Used to prevent notifications for current threadId
 */
object ActiveThreadId {
    var threadId: Long? = null
}