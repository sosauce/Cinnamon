@file:OptIn(ExperimentalUuidApi::class)

package com.sosauce.cinnamon.data.services

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.SubscriptionManager
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.managers.AndroidCallCallback
import com.sosauce.cinnamon.data.managers.CallManager
import com.sosauce.cinnamon.data.managers.CallNotificationManager
import com.sosauce.cinnamon.data.managers.CallServiceCallback
import com.sosauce.cinnamon.domain.model.AudioRoute
import com.sosauce.cinnamon.domain.model.CuteSimCard
import com.sosauce.cinnamon.domain.states.CallState
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi

class CallService: InCallService(), CallServiceCallback, AndroidCallCallback, KoinComponent {


    private lateinit var audioManager: AudioManager
    val callNotificationManager by inject<CallNotificationManager>()
    val callManager by inject<CallManager>()
    private var cuteCall: Call? = null

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        var i = 0L
        override fun run() {
            i++
            callManager.updateTimeSpent(i)
            handler.postDelayed(this, 1000)
        }
    }

    private val callback = object : Call.Callback() {

        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)

            when (state) {
                Call.STATE_RINGING -> {
                    callManager.updateCallState(CallState.RINGING)
                    callNotificationManager.createIncomingNotification(call.details)
                }
                Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                    callManager.updateCallState(CallState.DIALING)
                    callNotificationManager.createOutgoingNotification(call.details)
                }
                Call.STATE_ACTIVE -> {
                    handler.post(runnable)
                    callManager.updateCallState(CallState.ONGOING)
                    callNotificationManager.createOngoingNotification(call.details)
                }
                Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> callManager.updateCallState(CallState.ENDED)
                Call.STATE_HOLDING -> callManager.updateIsHolding(true)
                else -> return
            }

            callManager.updateIsHolding(state == Call.STATE_HOLDING)
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)
            callManager.updateNumber(details?.handle?.schemeSpecificPart ?: getString(R.string.unknown))
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



    @SuppressLint("MissingPermission")
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        cuteCall = call

        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            call.state
        }

        val telecomManager = getSystemService(TelecomManager::class.java)
        val subscriptionManager = getSystemService(SubscriptionManager::class.java)


        val subId = call.details.accountHandle?.id?.toIntOrNull() ?: -1
        val activeSubInfo = subscriptionManager.getActiveSubscriptionInfo(subId)
        val sim = CuteSimCard(
            subId = activeSubInfo.subscriptionId,
            name = activeSubInfo.displayName.toString(),
            carrierName = activeSubInfo.carrierName.toString(),
            color = activeSubInfo.iconTint
        )

        callManager.updateActiveSim(sim)


        val notification = when (state) {
            Call.STATE_RINGING -> {
                callManager.updateCallState(CallState.RINGING)
                callManager.updateNumber(call.details?.handle?.schemeSpecificPart ?: getString(R.string.unknown))
                callNotificationManager.createIncomingNotification(call.details)
            }
            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                callManager.updateCallState(CallState.DIALING)
                callNotificationManager.createOutgoingNotification(call.details)
            }
            Call.STATE_ACTIVE -> {
                handler.post(runnable)
                callManager.updateCallState(CallState.ONGOING)
                callNotificationManager.createOngoingNotification(call.details)
            }
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                callManager.updateCallState(CallState.ENDED)
                null
            }
            Call.STATE_HOLDING -> {
                callManager.updateIsHolding(true)
                null
            }
            else -> null
        }

        notification?.let { startForeground(CallNotificationManager.CALL_NOTIF_ID, it) }
        callManager.registerCallServiceCallback(this)
        callManager.registerAndroidCallCallback(this)
        cuteCall?.registerCallback(callback)
    }


    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        cuteCall?.unregisterCallback(callback)
        callManager.unregisterCallServiceCallback()
        callManager.unregisterAndroidCallCallback()
        handler.removeCallbacks(runnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }


    // A13 and below
    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        callManager.updateIsMuted(audioState?.isMuted == true)



        val supportedRoutes = audioState?.supportedRouteMask ?: 0
        val availableRoutes = listOf(
            CallAudioState.ROUTE_BLUETOOTH,
            CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_SPEAKER,
            CallAudioState.ROUTE_WIRED_HEADSET
        ).mapNotNull { route ->
            if (supportedRoutes and route != 0) {
                AudioRoute(
                    name = CallAudioState.audioRouteToString(route),
                    type = route
                )
            } else null
        }
        val endpoint = AudioRoute(
            name = CallAudioState.audioRouteToString(audioState?.route ?: CallAudioState.ROUTE_EARPIECE),
            type = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        )
        callManager.updateAvailableAudioRoutes(availableRoutes)
        callManager.updateCurrentAudioRoute(endpoint)
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
