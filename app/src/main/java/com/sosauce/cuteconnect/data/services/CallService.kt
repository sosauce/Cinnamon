@file:OptIn(ExperimentalUuidApi::class)

package com.sosauce.cuteconnect.data.services

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.InCallService
import android.telecom.PhoneAccountHandle
import android.telecom.VideoProfile
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.sosauce.cuteconnect.activities.CallActivity
import com.sosauce.cuteconnect.data.managers.AndroidCallCallback
import com.sosauce.cuteconnect.data.managers.CallManager
import com.sosauce.cuteconnect.main.MainActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.sosauce.cuteconnect.data.managers.CallNotificationManager
import com.sosauce.cuteconnect.data.managers.CallServiceCallback
import com.sosauce.cuteconnect.data.receivers.CallReceiver
import com.sosauce.cuteconnect.domain.model.AudioRoute
import com.sosauce.cuteconnect.domain.states.CallState
import com.sosauce.cuteconnect.utils.ACCEPT_INCOMING_CALL
import com.sosauce.cuteconnect.utils.DECLINE_INCOMING_CALL
import com.sosauce.cuteconnect.utils.HANGUP_ONGOING_CALL
import com.sosauce.cuteconnect.utils.SWITCH_AUDIO_SOURCE
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CallService: InCallService(), CallServiceCallback, AndroidCallCallback {


    private lateinit var audioManager: AudioManager
    val callNotificationManager by lazy { CallNotificationManager(this) }
    private var cuteCall: Call? = null
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        var i = 0L
        override fun run() {
            i++
            CallManager.updateTimeSpent(i)
            handler.postDelayed(this, 1000)
        }
    }

    private val callback = object : Call.Callback() {

        @SuppressLint("MissingPermission")
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)

            val notification = when(state) {
                Call.STATE_ACTIVE -> callNotificationManager.createOngoingBuilder(call)
                Call.STATE_RINGING, Call.STATE_DIALING -> callNotificationManager.createOutgoingBuilder(call)
                else -> callNotificationManager.createOngoingBuilder(call)
            }

            val callState = when(state) {
                Call.STATE_ACTIVE -> CallState.ONGOING
                Call.STATE_RINGING -> CallState.RINGING
                Call.STATE_DIALING -> CallState.DIALING
                Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallState.ENDED
                else -> CallState.ONGOING
            }
            CallManager.updateCallState(callState)
            CallManager.updateIsHolding(state == Call.STATE_HOLDING)

            callNotificationManager.sendNotification(notification)

        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)
            CallManager.updateNumber(details?.handle?.schemeSpecificPart ?: "Undetermined")
        }


    }

    private val audioFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build()
        )
        .build()

    override fun onCreate() {
        super.onCreate()
        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager).apply {
            requestAudioFocus(audioFocus)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.abandonAudioFocusRequest(audioFocus)

    }



    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        cuteCall = call
        CallManager.registerCallServiceCallback(this)
        CallManager.registerAndroidCallCallback(this)
        cuteCall?.registerCallback(callback)
        handler.post(runnable)



        // Launch the CallActivity whenever we get a call, no matter the state of the application
        val intent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)

        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                call.details.state == Call.STATE_RINGING
            } else {
                call.state == Call.STATE_RINGING
            }

        if (isIncoming) {
            val isScreenLocked = (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked

            val notification = callNotificationManager.createIncomingBuilder(call.details)
            callNotificationManager.sendNotification(notification)

            if (isScreenLocked) {
                val intent = Intent(this, CallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                startActivity(intent)
            }

        }

        println("hey im cool: $cuteCall")
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        cuteCall?.unregisterCallback(callback)
        CallManager.unregisterCallServiceCallback()
        CallManager.unregisterAndroidCallCallback()
        //handler.removeCallbacks(runnable)
        callNotificationManager.clearCallNotifications()
    }

    // A14+ ???
//    override fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint?>) {
//        super.onAvailableCallEndpointsChanged(availableEndpoints)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            val endpoints = availableEndpoints.map {
//                CompatCallEndpoint(
//                    id = it?.identifier ?: ParcelUuid.fromString(Uuid.random().toString()),
//                    name = it?.endpointName?.toString() ?: "No name",
//                    type = it?.endpointType ?: CallEndpoint.TYPE_EARPIECE
//                )
//            }
//            CallManager.updateAvailableEndpoints(endpoints)
//        }
//    }


    // Android 14+
//    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
//        super.onCallEndpointChanged(callEndpoint)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            CallManager.updateCurrentEndpoint(
//                endpoint = CompatCallEndpoint(
//                    name = callEndpoint.endpointName.toString(),
//                    type = callEndpoint.endpointType,
//                    id = callEndpoint.identifier
//                )
//            )
//        }
//    }


    // A13 and below
    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.updateIsMuted(audioState?.isMuted == true)



        val supportedRoutes = audioState?.supportedRouteMask ?: 0
        val availableRoutes = mutableListOf<AudioRoute>().apply {
            if (supportedRoutes and CallAudioState.ROUTE_BLUETOOTH != 0) {
                add(
                    AudioRoute(
                        name = CallAudioState.audioRouteToString(CallAudioState.ROUTE_BLUETOOTH),
                        type = CallAudioState.ROUTE_BLUETOOTH
                    )
                )
            }
            if (supportedRoutes and CallAudioState.ROUTE_EARPIECE != 0) {
                add(
                    AudioRoute(
                        name = CallAudioState.audioRouteToString(CallAudioState.ROUTE_EARPIECE),
                        type = CallAudioState.ROUTE_EARPIECE
                    )
                )
            }
            if (supportedRoutes and CallAudioState.ROUTE_SPEAKER != 0) {
                add(
                    AudioRoute(
                        name = CallAudioState.audioRouteToString(CallAudioState.ROUTE_SPEAKER),
                        type = CallAudioState.ROUTE_SPEAKER
                    )
                )
            }
            if (supportedRoutes and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
                add(
                    AudioRoute(
                        name = CallAudioState.audioRouteToString(CallAudioState.ROUTE_WIRED_HEADSET),
                        type = CallAudioState.ROUTE_WIRED_HEADSET
                    )
                )
            }
        }
        val endpoint = AudioRoute(
            name = CallAudioState.audioRouteToString(audioState?.route ?: CallAudioState.ROUTE_EARPIECE),
            type = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        )
        CallManager.updateAvailableAudioRoutes(availableRoutes)
        CallManager.updateCurrentAudioRoute(endpoint)
    }

    override fun toggleMute(mute: Boolean) = setMuted(mute)

    override fun switchAudioRoute(route: AudioRoute) = setAudioRoute(route.type)

    override fun answerCall() {
        cuteCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    override fun declineCall() {
        cuteCall?.reject(false, null)
    }

    override fun hangupOngoingCall() {
        cuteCall?.disconnect()
    }

    override fun startTone(char: Char) {
        cuteCall?.playDtmfTone(char)
        cuteCall?.stopDtmfTone()
    }

    override fun toggleHold() {
        val isHolding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            cuteCall?.details?.state == Call.STATE_HOLDING
        } else cuteCall?.state == Call.STATE_HOLDING

        if (isHolding) {
            cuteCall?.unhold()
        } else {
            cuteCall?.hold()
        }
    }
}
