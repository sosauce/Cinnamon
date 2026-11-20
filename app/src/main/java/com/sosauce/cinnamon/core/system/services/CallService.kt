@file:OptIn(ExperimentalUuidApi::class)

package com.sosauce.cinnamon.core.system.services

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.SubscriptionManager
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.PreferencesKeys.INCOMING_CALL_FULLSCREEN
import com.sosauce.cinnamon.core.datastore.dataStore
import com.sosauce.cinnamon.core.repository.SimsRepository
import com.sosauce.cinnamon.core.telecomContactName
import com.sosauce.cinnamon.core.telecomContactPhotoUri
import com.sosauce.cinnamon.core.telephony.phone.AndroidCallCallback
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.core.telephony.phone.CallNotificationManager
import com.sosauce.cinnamon.core.telephony.phone.CallServiceCallback
import com.sosauce.cinnamon.core.utils.safeStartActivity
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.cinnamon.features.phone.presentation.call.CallActivity
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

/**
 * Tracks every call Telecom gives us (call waiting = 2+ calls). One "primary" call is shown in the
 * call UI / notification and receives the UI actions, the next one is exposed as [com.sosauce.cinnamon.features.phone.presentation.call.OtherCall].
 *
 * All Telecom callbacks, the timer and [refreshCalls] run on the main thread.
 */
class CallService : InCallService(), CallServiceCallback, AndroidCallCallback, KoinComponent {


    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)
    val callNotificationManager by inject<CallNotificationManager>()
    val callManager by inject<CallManager>()
    val callOverlayManager by inject<CallOverlayManager>()
    val simsRepository by inject<SimsRepository>()
    val telecomManager by inject<TelecomManager>()

    // Call currently shown in the UI, read from the notification coroutine too
    @Volatile
    private var primaryCall: Call? = null
    private var primaryNumber: String? = null
    private var primaryTelecomName: String? = null
    // Same for the photo, which is loaded in the background by photoJob
    private var primaryTelecomPhoto: Uri? = null
    private var photoJob: Job? = null

    // (call, notification type) last posted, so we don't re-post (and restart the ringtone) on every refresh
    private var notifiedKey: Pair<Call, NotificationType>? = null
    private var notificationJob: Job? = null

    // Call whose UI should be launched once its first notification is posted
    @Volatile
    private var launchUiFor: Call? = null

    // "End & answer": answer this ringing call once the calls we ended are gone
    private var pendingAnswerCall: Call? = null
    private var endedForAnswer: Set<Call> = emptySet()

    // Fallback start time for calls whose details don't expose connectTimeMillis
    private val localConnectTimes = mutableMapOf<Call, Long>()

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            updateTimers()
            handler.postDelayed(this, 1000)
        }
    }

    private val pendingAnswerTimeout = Runnable {
        val pending = pendingAnswerCall ?: return@Runnable
        pendingAnswerCall = null
        endedForAnswer = emptySet()
        if (pending.currentState() == Call.STATE_RINGING) {
            pending.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    private val callback = object : Call.Callback() {

        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            refreshCalls()
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)
            refreshCalls()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            callOverlayManager.observe(scope)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //audioManager.abandonAudioFocusRequest(audioFocus)
        try {
            callOverlayManager.hideOverlay()
        } catch (_: Exception) {
        }
        handler.removeCallbacks(runnable)
        handler.removeCallbacks(pendingAnswerTimeout)
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
        safeStartActivity(intent)
    }


    @SuppressLint("MissingPermission")
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        callManager.registerCallServiceCallback(this)
        callManager.registerAndroidCallCallback(this)
        call.registerCallback(callback)

        when (call.currentState()) {
            Call.STATE_RINGING, Call.STATE_DIALING, Call.STATE_CONNECTING -> launchUiFor = call

            Call.STATE_SELECT_PHONE_ACCOUNT -> {
                // Fallback if Telecom still asks to select SIM - pick first available and proceed
                // This prevents system SIM chooser from opening default dialer
                try {
                    @Suppress("MissingPermission")
                    val tm = getSystemService(android.telecom.TelecomManager::class.java)
                    val firstHandle =
                        tm.callCapablePhoneAccounts?.firstOrNull()
                    if (firstHandle != null) {
                        call.phoneAccountSelected(firstHandle, false)
                    } else {
                        call.disconnect()
                    }
                } catch (_: Exception) {
                    call.disconnect()
                }
            }
        }

        refreshCalls()
    }


    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        call?.unregisterCallback(callback)
        localConnectTimes.remove(call)

        if (calls.none { it != call }) {
            tearDown()
        } else {
            refreshCalls()
        }
    }

    private fun tearDown() {
        callManager.unregisterCallServiceCallback()
        callManager.unregisterAndroidCallCallback()
        handler.removeCallbacks(runnable)
        handler.removeCallbacks(pendingAnswerTimeout)
        notificationJob?.cancel()
        notificationJob = null
        notifiedKey = null
        primaryCall = null
        primaryNumber = null
        primaryTelecomName = null
        primaryTelecomPhoto = null
        photoJob = null
        launchUiFor = null
        pendingAnswerCall = null
        endedForAnswer = emptySet()
        localConnectTimes.clear()
        callManager.updateTimeSpent(0L)
        callManager.updateCallState(CallState.ENDED)
        job.cancelChildren()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Re-computes the primary/other call from [getCalls] and pushes everything to [CallManager].
     */
    private fun refreshCalls() {
        val live = liveCalls()
        handlePendingAnswer(live)

        val primary = live.firstOrNull()
        if (primary == null) {
            // Every remaining call is disconnecting, onCallRemoved will tear down
            primaryCall = null
            primaryNumber = null
            primaryTelecomName = null
            primaryTelecomPhoto = null
            photoJob?.cancel()
            notificationJob?.cancel()
            notifiedKey = null
            handler.removeCallbacks(runnable)
            callManager.updateCallState(CallState.ENDED)
            return
        }

        val primaryChanged = primary != primaryCall
        primaryCall = primary
        val state = primary.currentState()

        if (primaryChanged) callManager.updateActiveSim(simFor(primary))

        val number = primary.rawNumber()
        val numberChanged = primaryChanged || number != primaryNumber
        primaryNumber = number
        val details = primary.details

        val telecomName = details?.telecomContactName
        if (numberChanged || telecomName != primaryTelecomName) {
            primaryTelecomName = telecomName
            callManager.updateNumber(number, details)
        }

        val telecomPhoto = details?.telecomContactPhotoUri
        if (numberChanged || telecomPhoto != primaryTelecomPhoto) {
            primaryTelecomPhoto = telecomPhoto
            photoJob?.cancel()
            photoJob = scope.launch { callManager.updatePhoto(number, details) }
        }

        callManager.updateCallState(
            when (state) {
                Call.STATE_RINGING -> CallState.RINGING
                Call.STATE_ACTIVE, Call.STATE_HOLDING -> CallState.ONGOING
                else -> CallState.DIALING
            }
        )
        callManager.updateIsHolding(state == Call.STATE_HOLDING)
        primary.details?.let { details ->
            callManager.updateCallProperties(
                isHdAudio = details.hasProperty(Call.Details.PROPERTY_HIGH_DEF_AUDIO),
                isWifiCall = details.hasProperty(Call.Details.PROPERTY_WIFI)
            )
        }

        val other = live.getOrNull(1)
        callManager.updateOtherCall(
            number = other?.rawNumber(),
            details = other?.details,
            isHolding = other?.currentState() == Call.STATE_HOLDING,
            timeSpentInCall = other?.elapsedSeconds() ?: 0L
        )

        // Timer: derived from each call's connect time, so hold/unhold/swap never resets it
        handler.removeCallbacks(runnable)
        if (live.fastAny { it.currentState() == Call.STATE_ACTIVE || it.currentState() == Call.STATE_HOLDING }) {
            handler.post(runnable)
        } else {
            callManager.updateTimeSpent(0L)
        }

        val notificationType = when (state) {
            Call.STATE_RINGING -> NotificationType.INCOMING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> NotificationType.OUTGOING
            Call.STATE_ACTIVE, Call.STATE_HOLDING -> NotificationType.ONGOING
            else -> null
        }
        val key = notificationType?.let { primary to it }
        if (key != null && key != notifiedKey) {
            notifiedKey = key
            postNotification(primary, notificationType)
        }
    }

    private fun postNotification(call: Call, type: NotificationType) {
        notificationJob?.cancel()
        notificationJob = scope.launch {
            val useFullScreen = type != NotificationType.INCOMING || try {
                applicationContext.dataStore.data.first()[INCOMING_CALL_FULLSCREEN]
                    ?: true
            } catch (_: Exception) {
                true
            }
            // Check incoming call popup setting — if disabled, show bubble/notification only, not full-screen

            val notification: Notification? = when (type) {
                NotificationType.INCOMING -> callNotificationManager.createIncomingNotification(
                    call.details,
                    useFullScreen
                )

                NotificationType.OUTGOING -> callNotificationManager.createOutgoingNotification(call.details)
                NotificationType.ONGOING -> callNotificationManager.createOngoingNotification(call.details)
            }
            ensureActive()
            // Notification first, to ensure foreground priority on Android 10+ before launching the UI
            notification?.let { startForeground(CallNotificationManager.CALL_NOTIF_ID, it) }

            if (launchUiFor == call) {
                launchUiFor = null
                // Only launch full-screen CallActivity for incoming calls if setting enabled
                // Otherwise, bubble overlay (CallOverlayManager) will show as popup over other apps
                if (useFullScreen) launchCallActivity()
            }
        }
    }

    private fun updateTimers() {
        callManager.updateTimeSpent(primaryCall?.elapsedSeconds() ?: 0L)
        liveCalls().getOrNull(1)?.let { callManager.updateOtherCallTime(it.elapsedSeconds()) }
    }

    private fun handlePendingAnswer(live: List<Call>) {
        val pending = pendingAnswerCall ?: return
        if (pending !in live || pending.currentState() != Call.STATE_RINGING) {
            pendingAnswerCall = null
            endedForAnswer = emptySet()
            handler.removeCallbacks(pendingAnswerTimeout)
            return
        }
        if (live.none { it in endedForAnswer }) {
            pendingAnswerCall = null
            endedForAnswer = emptySet()
            handler.removeCallbacks(pendingAnswerTimeout)
            pending.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    /**
     * Calls that aren't disconnecting/disconnected, most important first: ringing > active > dialing > held.
     * Calls with the same priority keep Telecom's order (oldest first).
     */
    private fun liveCalls(): List<Call> = calls
        .mapNotNull { call -> callPriority(call.currentState())?.let { call to it } }
        .sortedBy { it.second }
        .map { it.first }

    private fun callPriority(state: Int): Int? = when (state) {
        Call.STATE_RINGING -> 0
        Call.STATE_ACTIVE -> 1
        Call.STATE_HOLDING -> 3
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> null
        else -> 2 // dialing, connecting, selecting SIM, new...
    }

    private fun ringingCall(): Call? = calls.firstOrNull { it.currentState() == Call.STATE_RINGING }

    private fun Call.currentState(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        details.state
    } else {
        @Suppress("DEPRECATION")
        state
    }

    private fun Call.rawNumber(): String =
        details?.handle?.schemeSpecificPart ?: getString(R.string.unknown)

    private fun Call.elapsedSeconds(): Long {
        val state = currentState()
        if (state != Call.STATE_ACTIVE && state != Call.STATE_HOLDING) return 0L
        val connectTime = details?.connectTimeMillis?.takeIf { it > 0 }
            ?: localConnectTimes.getOrPut(this) { System.currentTimeMillis() }
        return ((System.currentTimeMillis() - connectTime) / 1000).coerceAtLeast(0L)
    }

    @SuppressLint("MissingPermission")
    private fun simFor(call: Call): CuteSimCard {


        //val phoneAccount = telecomManager.getPhoneAccount(call.details.accountHandle)

//        return CuteSimCard(
//            subId = phoneAccount.accountHandle.id,
//            name = phoneAccount.label.toString(),
//            carrierName = phoneAccount.accountHandle.userHandle,
//            color = phoneAccount.highlightColor
//        )

        val subscriptionManager = getSystemService(SubscriptionManager::class.java)
        val subId = call.details.accountHandle?.let { simsRepository.subIdFor(it) } ?: -1
        val activeSubInfo = try {
            subscriptionManager.getActiveSubscriptionInfo(subId)
        } catch (_: SecurityException) {
            null
        }
        return CuteSimCard(
            subId = activeSubInfo?.subscriptionId ?: subId,
            name = activeSubInfo?.displayName?.toString() ?: "SIM $subId",
            carrierName = activeSubInfo?.carrierName?.toString() ?: "",
            color = activeSubInfo?.iconTint ?: 0
        )
    }

    // A14 +
    override fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint?>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            val availableRoutes = availableEndpoints.fastMapNotNull { endpoint ->
                if (endpoint == null) {
                    null
                }  else {
                    AudioRoute(
                        name = endpoint.endpointName.toString(),
                        type = endpoint.endpointType
                    )
                }

            }
            callManager.updateAvailableAudioRoutes(availableRoutes)
        }
    }

    // A14 +
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val route = AudioRoute(
                name = callEndpoint.endpointName.toString(),
                type = callEndpoint.endpointType
            )
            callManager.updateCurrentAudioRoute(route)
        }
    }


    // A14 +
    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        callManager.updateIsMuted(isMuted)
    }

//    // A13 and below
//    @Deprecated("Deprecated in Java")
//    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
//        super.onCallAudioStateChanged(audioState)
//        callManager.updateIsMuted(audioState?.isMuted == true)
//
//
//        val supportedRoutes = audioState?.supportedRouteMask ?: 0
//        val availableRoutes = listOf(
//            CallAudioState.ROUTE_BLUETOOTH,
//            CallAudioState.ROUTE_EARPIECE,
//            CallAudioState.ROUTE_SPEAKER,
//            CallAudioState.ROUTE_WIRED_HEADSET
//        ).mapNotNull { route ->
//            if (supportedRoutes and route != 0) {
//                AudioRoute(
//                    name = CallAudioState.audioRouteToString(route),
//                    type = route
//                )
//            } else null
//        }
//        val endpoint = AudioRoute(
//            name = CallAudioState.audioRouteToString(
//                audioState?.route ?: CallAudioState.ROUTE_EARPIECE
//            ),
//            type = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
//        )
//        callManager.updateAvailableAudioRoutes(availableRoutes)
//        callManager.updateCurrentAudioRoute(endpoint)
//    }

    override fun toggleMute(mute: Boolean) = setMuted(mute)

    override fun switchAudioRoute(route: AudioRoute) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            requestCallEndpointChange(
//                CallEndpoint(
//                    route.name, route.type
//                ),
//                mainExecutor,
//                object : OutcomeReceiver<Void, CallEndpointException> {
//                    override fun onResult(result: Void?) = Unit
//                    override fun onError(error: CallEndpointException) = Unit
//                }
//        } else {
//        }
        setAudioRoute(route.type)
    }

    override fun answerCall() {
        ringingCall()?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    override fun declineCall() {
        ringingCall()?.reject(false, null)
    }

    override fun holdAndAnswer() {
        // Telecom holds the current active call itself when answering a waiting call
        ringingCall()?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    override fun endAndAnswer() {
        val ringing = ringingCall() ?: return
        val others = liveCalls().filter { it != ringing }
        // End the foreground call only, a call already on hold stays on hold
        val toEnd = others.filter { it.currentState() == Call.STATE_ACTIVE }.ifEmpty { others }

        if (toEnd.isEmpty()) {
            ringing.answer(VideoProfile.STATE_AUDIO_ONLY)
            return
        }

        // Answer once the ended calls are gone (see handlePendingAnswer), otherwise Telecom
        // would try to hold the call we're disconnecting.
        pendingAnswerCall = ringing
        endedForAnswer = toEnd.toSet()
        handler.removeCallbacks(pendingAnswerTimeout)
        handler.postDelayed(pendingAnswerTimeout, PENDING_ANSWER_TIMEOUT_MILLIS)
        toEnd.forEach { it.disconnect() }
    }

    override fun swapCalls() {
        val primary = primaryCall
        // Unholding the held call makes Telecom hold the currently active one
        val held = calls.firstOrNull { it != primary && it.currentState() == Call.STATE_HOLDING }
            ?: primary?.takeIf { it.currentState() == Call.STATE_HOLDING }
        held?.unhold()
    }

    override fun hangupOngoingCall() {
        // Try primary call first, then any call in InCallService's call list
        // Fixes "end call button not working" when primaryCall is null (optimistic UI)
        // or when Telecom hasn't yet delivered the call.
        val primary = primaryCall
        val target = primary ?: calls.firstOrNull()
        if (target != null) {
            target.disconnect()
        } else {
            // No Telecom call to disconnect — force UI to ENDED so CallActivity finishes
            callManager.updateCallState(CallState.ENDED)
        }
        // Always ensure state moves to ENDED for immediate UI feedback
        // (CallService will also get STATE_DISCONNECTED callback)
        if (primary == null) callManager.updateCallState(CallState.ENDED)
    }

    override suspend fun startTone(char: Char) {
        val call = primaryCall
        call?.playDtmfTone(char)
        delay(DTMF_TONE_DURATION_MILLIS.milliseconds)
        call?.stopDtmfTone()
    }

    override fun toggleHold() {
        val call = primaryCall ?: return
        if (call.currentState() == Call.STATE_HOLDING) {
            call.unhold()
        } else {
            call.hold()
        }
    }

    private enum class NotificationType { INCOMING, OUTGOING, ONGOING }

    companion object {
        private const val DTMF_TONE_DURATION_MILLIS = 150
        private const val PENDING_ANSWER_TIMEOUT_MILLIS = 3000L
    }
}
