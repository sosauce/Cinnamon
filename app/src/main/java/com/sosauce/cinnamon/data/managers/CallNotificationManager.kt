package com.sosauce.cinnamon.data.managers

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.activities.CallActivity
import com.sosauce.cinnamon.data.receivers.CallReceiver
import com.sosauce.cinnamon.utils.ACCEPT_INCOMING_CALL
import com.sosauce.cinnamon.utils.DECLINE_INCOMING_CALL
import com.sosauce.cinnamon.utils.HANGUP_ONGOING_CALL
import com.sosauce.cinnamon.utils.getContactNameOrNothing

class CallNotificationManager(
    private val context: Context,
) {

    val notificationManager = NotificationManagerCompat.from(context)

    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        flags = Intent.FLAG_ACTIVITY_NO_USER_ACTION
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        setClass(context, CallActivity::class.java)
    }

    val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_MUTABLE)


    private val declineIntent = Intent(context, CallReceiver::class.java).apply {
        action = DECLINE_INCOMING_CALL
    }
    private val acceptIntent = Intent(context, CallReceiver::class.java).apply {
        action = ACCEPT_INCOMING_CALL
    }
    private val hangupIntent = Intent(context, CallReceiver::class.java).apply {
        action = HANGUP_ONGOING_CALL
    }

    private val declinePendingIntent = PendingIntent.getBroadcast(
        context,
        DECLINE_CALL_CODE,
        declineIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    private val acceptPendingIntent = PendingIntent.getBroadcast(
        context,
        ACCEPT_CALL_CODE,
        acceptIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    private val hangupPendingIntent = PendingIntent.getBroadcast(
        context,
        HANGUP_ONGOING_CALL_CODE,
        hangupIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )


    @SuppressLint("MissingPermission")
    fun createIncomingNotification(
        callDetails: Call.Details
    ): Notification {

        val number = callDetails.gatewayInfo?.originalAddress?.schemeSpecificPart
            ?: callDetails.handle.schemeSpecificPart

        //val contactPfpUri = number.getContactPfpFromNumber(context)
        val contactPfpUri = Uri.EMPTY
        val icon = if (contactPfpUri != Uri.EMPTY) {
            IconCompat.createWithContentUri(contactPfpUri)
        } else IconCompat.createWithResource(context, R.drawable.account)


        val builder = NotificationCompat.Builder(context, CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_call_received_24)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    Person.Builder()
                        .setIcon(icon)
                        .setName(number.getContactNameOrNothing(context))
                        .build(),
                    declinePendingIntent,
                    acceptPendingIntent

                )
            )
            .build()

        notificationManager.notify(CALL_NOTIF_ID, builder)

        return builder
    }

    @SuppressLint("MissingPermission")
    fun createOngoingNotification(
        callDetails: Call.Details
    ): Notification {

        val number = callDetails.gatewayInfo?.originalAddress?.schemeSpecificPart
            ?: callDetails.handle.schemeSpecificPart

        //val contactPfpUri = number.getContactPfpFromNumber(context)
        val contactPfpUri = Uri.EMPTY
        val icon = if (contactPfpUri != Uri.EMPTY) {
            IconCompat.createWithContentUri(contactPfpUri)
        } else IconCompat.createWithResource(context, R.drawable.account)


        val builder = NotificationCompat.Builder(context, CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.call)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setContentTitle("Hello world")
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setUsesChronometer(true)
            .setFullScreenIntent(pendingIntent, true)
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    Person.Builder()
                        .setIcon(icon)
                        .setName(number.getContactNameOrNothing(context))
                        .build(),
                    hangupPendingIntent

                )
            )
            .build()

        notificationManager.notify(CALL_NOTIF_ID, builder)


        return builder
    }

    @SuppressLint("MissingPermission")
    fun createOutgoingNotification(
        callDetails: Call.Details
    ): Notification {

        val number = callDetails.gatewayInfo?.originalAddress?.schemeSpecificPart
            ?: callDetails.handle.schemeSpecificPart

        //val contactPfpUri = number.getContactPfpFromNumber(context)
        val contactPfpUri = Uri.EMPTY
        val icon = if (contactPfpUri != Uri.EMPTY) {
            IconCompat.createWithContentUri(contactPfpUri)
        } else IconCompat.createWithResource(context, R.drawable.account)


        val builder = NotificationCompat.Builder(context, CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_call_made_24)
            .setCategory(Notification.CATEGORY_CALL)
            .setContentText(context.getString(R.string.ringing))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setFullScreenIntent(pendingIntent, false)
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    Person.Builder()
                        .setIcon(icon)
                        .setName(number.getContactNameOrNothing(context))
                        .build(),
                    hangupPendingIntent

                )
            )
            .build()

        notificationManager.notify(CALL_NOTIF_ID, builder)

        return builder
    }


    companion object {
        private const val DECLINE_CALL_CODE = 0
        private const val ACCEPT_CALL_CODE = 1
        private const val HANGUP_ONGOING_CALL_CODE = 2
        const val CALLS_CHANNEL_ID = "calls_id"
        const val CALLS_GROUP = "calls_group"

        const val CALL_NOTIF_ID = 1
    }
}

