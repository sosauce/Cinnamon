@file:OptIn(ExperimentalUuidApi::class)

package com.sosauce.cinnamon.core.system.services

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.telephony.SubscriptionManager
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.dataStore
import com.sosauce.cinnamon.features.phone.presentation.call.CallActivity
import com.sosauce.cinnamon.core.telephony.phone.AndroidCallCallback
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.core.telephony.phone.CallNotificationManager
import com.sosauce.cinnamon.core.telephony.phone.CallServiceCallback
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi

class CallService : InCallService(), CallServiceCallback, AndroidCallCallback, KoinComponent {


    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)
    private lateinit var audioManager: AudioManager
    val callNotificationManager by inject<CallNotificationManager>()
    val callManager by inject<CallManager>()
    val callOverlayManager by inject<CallOverlayManager>()
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

            scope.launch {
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

                    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> callManager.updateCallState(
                        CallState.ENDED
                    )

                    Call.STATE_HOLDING -> callManager.updateIsHolding(true)
                    else -> return@launch
                }
            }


            callManager.updateIsHolding(state == Call.STATE_HOLDING)
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)
            callManager.updateNumber(
                details?.handle?.schemeSpecificPart ?: getString(R.string.unknown)
            )
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
        // Start observing call state to show/hide bubble overlay over other apps
        // M3 Expressive blur background via tonal surface in CallBubble
        try {
            callOverlayManager.observe(scope)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.abandonAudioFocusRequest(audioFocus)
        try { callOverlayManager.hideOverlay() } catch (_: Exception) {}
        job.cancel()
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        launchCallActivity()
    }

    private fun launchCallActivity() {
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        // InCallService is allowed to start activity, but on Android 10+ background
        // launches are restricted — ensure we are foreground or use fullScreenIntent.
        // Callers (CallManager/ViewModel) also launch CallActivity directly as immediate UI,
        // this is a fallback for Telecom-triggered calls (incoming / from other apps).
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: rely on notification fullScreenIntent
            android.util.Log.w("CallService", "launchCallActivity failed, relying on fullScreenIntent", e)
        }
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

        val subscriptionManager = getSystemService(SubscriptionManager::class.java)


        val subId = call.details.accountHandle?.id?.toIntOrNull() ?: -1
        val activeSubInfo = try {
            subscriptionManager.getActiveSubscriptionInfo(subId)
        } catch (_: SecurityException) { null }
        val sim = CuteSimCard(
            subId = activeSubInfo?.subscriptionId ?: subId,
            name = activeSubInfo?.displayName?.toString() ?: "SIM $subId",
            carrierName = activeSubInfo?.carrierName?.toString() ?: "",
            color = activeSubInfo?.iconTint ?: 0
        )

        callManager.updateActiveSim(sim)


        scope.launch {
            // Check incoming call popup setting — if disabled, show bubble/notification only, not full-screen
            val useFullScreen = try {
                runBlocking {
                    applicationContext.dataStore.data.first()[com.sosauce.cinnamon.core.datastore.PreferencesKeys.INCOMING_CALL_FULLSCREEN] ?: true
                }
            } catch (_: Exception) { true }

            val notification = when (state) {
                Call.STATE_RINGING -> {
                    callManager.updateCallState(CallState.RINGING)
                    callManager.updateNumber(
                        call.details?.handle?.schemeSpecificPart ?: getString(R.string.unknown)
                    )
                    // For incoming, ensure foreground before fullScreenIntent
                    val notif = callNotificationManager.createIncomingNotification(call.details, useFullScreen)
                    // Only launch full-screen CallActivity if setting enabled
                    // Otherwise, bubble overlay (CallOverlayManager) will show as popup over other apps
                    if (useFullScreen) {
                        try { launchCallActivity() } catch (_: Exception) {}
                    }
                    notif
                }

                Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                    callManager.updateCallState(CallState.DIALING)
                    // Create notification first to ensure foreground priority on Android 10+
                    val notif = callNotificationManager.createOutgoingNotification(call.details)
                    launchCallActivity()
                    notif
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

                Call.STATE_SELECT_PHONE_ACCOUNT -> {
                    // Fallback if Telecom still asks to select SIM — pick first available and proceed
                    // This prevents system SIM chooser from opening default dialer
                    try {
                        @Suppress("MissingPermission")
                        val tm = getSystemService(android.telecom.TelecomManager::class.java)
                        val firstHandle = tm.callCapablePhoneAccounts?.firstOrNull() as? android.telecom.PhoneAccountHandle
                        if (firstHandle != null) {
                            call.phoneAccountSelected(firstHandle, false)
                        } else {
                            callManager.updateCallState(CallState.ENDED)
                        }
                    } catch (_: Exception) {
                        callManager.updateCallState(CallState.ENDED)
                    }
                    null
                }

                else -> null
            }
            notification?.let { startForeground(CallNotificationManager.CALL_NOTIF_ID, it) }
        }

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
        job.cancelChildren()
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
            name = CallAudioState.audioRouteToString(
                audioState?.route ?: CallAudioState.ROUTE_EARPIECE
            ),
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
        // Try primary call first, then any call in InCallService's call list
        // Fixes "end call button not working" when cuteCall is null (optimistic UI)
        // or when Telecom hasn't yet delivered the call to cuteCall.
        val target = cuteCall ?: try { calls.firstOrNull() } catch (_: Exception) { null }
        if (target != null) {
            try { target.disconnect() } catch (_: Exception) {}
        } else {
            // No Telecom call to disconnect — force UI to ENDED so CallActivity finishes
            callManager.updateCallState(CallState.ENDED)
        }
        // Always ensure state moves to ENDED for immediate UI feedback
        // (CallService will also get STATE_DISCONNECTED callback)
        try {
            if (cuteCall == null) callManager.updateCallState(CallState.ENDED)
        } catch (_: Exception) {}
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
