package com.sosauce.cinnamon.core.telephony.phone

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.system.receivers.CallReceiver
import com.sosauce.cinnamon.core.utils.ACCEPT_INCOMING_CALL
import com.sosauce.cinnamon.core.utils.DECLINE_INCOMING_CALL
import com.sosauce.cinnamon.core.utils.HANGUP_ONGOING_CALL
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.features.phone.presentation.call.CallActivity

class CallNotificationManager(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val numberLookup: NumberLookup
) {

    val intent = Intent(context, CallActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )


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
    suspend fun createIncomingNotification(
        callDetails: Call.Details,
        useFullScreen: Boolean = true
    ): Notification {
        val builder = NotificationCompat.Builder(context, CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_call_received_24)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, useFullScreen)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    callDetails.createPerson(),
                    declinePendingIntent,
                    acceptPendingIntent

                )
            )
            .build()

        notificationManager.notify(CALL_NOTIF_ID, builder)

        return builder
    }

    @SuppressLint("MissingPermission")
    suspend fun createOngoingNotification(
        callDetails: Call.Details
    ): Notification {

        val builder = NotificationCompat.Builder(context, CALLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.call)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setUsesChronometer(true)
            .apply {
                // Keep the chronometer right when re-posted (call waiting swap, other call ended…)
                if (callDetails.connectTimeMillis > 0) setWhen(callDetails.connectTimeMillis)
            }
            .setFullScreenIntent(pendingIntent, true)
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    callDetails.createPerson(),
                    hangupPendingIntent

                )
            )
            .build()

        notificationManager.notify(CALL_NOTIF_ID, builder)


        return builder
    }

    @SuppressLint("MissingPermission")
    suspend fun createOutgoingNotification(
        callDetails: Call.Details
    ): Notification {
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
                    callDetails.createPerson(),
                    hangupPendingIntent

                )
            )
            .build()

        notificationManager.notify(CALL_NOTIF_ID, builder)

        return builder
    }


    private fun Call.Details.callerNumber(): String =
        gatewayInfo?.originalAddress?.schemeSpecificPart ?: handle.schemeSpecificPart

    private suspend fun Call.Details.createPerson(): Person {

        val callerNumber = callerNumber()

        val name = numberLookup.fetchCallerName(this, callerNumber)
            ?: callerNumber.beautifyNumber()

        val icon = numberLookup.fetchPersonIcon(
            numberLookup.fetchCallerPhoto(this, callerNumber, fullQuality = false)
        )


        return Person.Builder()
            .setIcon(icon)
            .setName(name)
            .build()
    }


    companion object {
        private const val DECLINE_CALL_CODE = 0
        private const val ACCEPT_CALL_CODE = 1
        private const val HANGUP_ONGOING_CALL_CODE = 2
        const val CALLS_CHANNEL_ID = "calls_silent_id"

        // Old channel that used the ringtone as its sound, deleted on startup
        const val LEGACY_CALLS_CHANNEL_ID = "calls_id"
        const val CALLS_GROUP = "calls_group"

        const val CALL_NOTIF_ID = 1
    }
}

