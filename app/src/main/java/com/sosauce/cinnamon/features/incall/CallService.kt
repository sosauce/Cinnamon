package com.sosauce.cinnamon.features.incall

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import com.sosauce.cinnamon.core.datastore.PreferencesKeys.INCOMING_CALL_FULLSCREEN
import com.sosauce.cinnamon.core.datastore.dataStore
import com.sosauce.cinnamon.core.utils.safeStartActivity
import com.sosauce.cinnamon.features.incall.presentation.CallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

/**
 * Telecom's entry point: feeds calls to [InCallRepository], keeps the call notification (and
 * foreground state) up to date, shows the call UI and runs the audio commands.
 */
class CallService : InCallService(), KoinComponent, CallAudioController {

    private val inCallRepository by inject<InCallRepository>()
    private val callNotificationManager by inject<CallNotificationManager>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationJob: Job? = null

    // (call id, notification type) last posted, so we don't re-post on every update
    private var notifiedKey: Pair<String, NotificationType>? = null

    // Call whose UI should be launched once its first notification is posted
    private var launchUiFor: String? = null

    // A14+, needed to switch to one of them
    private var callEndpoints: List<CallEndpoint> = emptyList()

    private val callCallback = object : Call.Callback() {

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)
            if (call == null) return
            inCallRepository.onCallUpdated(call)
        }

        override fun onStateChanged(call: Call?, state: Int) {
            super.onStateChanged(call, state)
            if (call == null) return
            inCallRepository.onCallUpdated(call)
        }
    }

    override fun onCreate() {
        super.onCreate()
        inCallRepository.setAudioController(this)
        scope.launch {
            inCallRepository.inCallState.collect { state ->
                if (state is InCallState.Active) postNotificationFor(state.primaryCall)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inCallRepository.setAudioController(null)
        scope.cancel()
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        launchCallActivity()
    }

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        if (call == null) return
        call.registerCallback(callCallback)

        val state = call.compatState
        if (state == Call.STATE_SELECT_PHONE_ACCOUNT) selectFallbackPhoneAccount(call)

        val launchUi = when (state) {
            Call.STATE_ACTIVE, Call.STATE_HOLDING, Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> false
            else -> true // ringing, or an outgoing call that's still being set up
        }
        // Read by the notification job after its first suspension, so it's set in time
        val id = inCallRepository.onCallAdded(call)
        if (launchUi) launchUiFor = id
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        if (call == null) return
        call.unregisterCallback(callCallback)
        inCallRepository.onCallRemoved(call)

        if (calls.none { it != call }) tearDown()
    }

    private fun tearDown() {
        notificationJob?.cancel()
        notificationJob = null
        notifiedKey = null
        launchUiFor = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Fallback if Telecom still asks to select a SIM: pick the first one and proceed.
     * This prevents the system SIM chooser from opening the default dialer
     */
    @SuppressLint("MissingPermission")
    private fun selectFallbackPhoneAccount(call: Call) {
        try {
            val firstHandle = getSystemService(TelecomManager::class.java)
                .callCapablePhoneAccounts
                ?.firstOrNull()
            if (firstHandle != null) {
                call.phoneAccountSelected(firstHandle, false)
            } else {
                call.disconnect()
            }
        } catch (_: Exception) {
            call.disconnect()
        }
    }

    private fun postNotificationFor(primary: CallInfo) {
        val type = when (primary.state) {
            CallState.RINGING -> NotificationType.INCOMING
            CallState.DIALING, CallState.CONNECTING -> NotificationType.OUTGOING
            CallState.ACTIVE, CallState.HOLDING -> NotificationType.ONGOING
            else -> return
        }
        val key = primary.id to type
        if (key == notifiedKey) return
        val details = inCallRepository.callFor(primary.id)?.details ?: return
        notifiedKey = key

        notificationJob?.cancel()
        notificationJob = scope.launch {
            // If the incoming call popup is disabled, show the notification only, not the full screen UI
            val useFullScreen = type != NotificationType.INCOMING || try {
                applicationContext.dataStore.data.first()[INCOMING_CALL_FULLSCREEN] ?: true
            } catch (_: Exception) {
                true
            }

            val notification = when (type) {
                NotificationType.INCOMING -> callNotificationManager.createIncomingNotification(details, useFullScreen)
                NotificationType.OUTGOING -> callNotificationManager.createOutgoingNotification(details)
                NotificationType.ONGOING -> callNotificationManager.createOngoingNotification(details)
            }
            ensureActive()
            // Notification first, to ensure foreground priority on Android 10+ before launching the UI
            startForeground(CallNotificationManager.CALL_NOTIF_ID, notification)

            if (launchUiFor == primary.id) {
                launchUiFor = null
                if (useFullScreen) launchCallActivity()
            }
        }
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


    override fun mute(muted: Boolean) = setMuted(muted)

    override fun switchAudioRoute(route: AudioRoute) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val endpoint = callEndpoints.firstOrNull { it.toAudioRoute() == route }
                ?: callEndpoints.firstOrNull { it.toAudioRoute().type == route.type }
            if (endpoint != null) {
                requestCallEndpointChange(
                    endpoint,
                    mainExecutor,
                    object : OutcomeReceiver<Void?, CallEndpointException> {
                        override fun onResult(result: Void?) = Unit
                        override fun onError(error: CallEndpointException) = Unit
                    }
                )
                return
            }
        }
        @Suppress("DEPRECATION")
        setAudioRoute(route.type)
    }

    // A14+
    override fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            callEndpoints = availableEndpoints
            inCallRepository.onAudioChanged(
                inCallRepository.callAudio.value.copy(availableRoutes = availableEndpoints.map { it.toAudioRoute() })
            )
        }
    }

    // A14+
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            inCallRepository.onAudioChanged(
                inCallRepository.callAudio.value.copy(currentRoute = callEndpoint.toAudioRoute())
            )
        }
    }

    // A14+
    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        inCallRepository.onAudioChanged(inCallRepository.callAudio.value.copy(isMuted = isMuted))
    }

    // A13 and below, A14+ uses the endpoint/mute callbacks above
    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || audioState == null) return

        val availableRoutes = listOf(
            CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_BLUETOOTH,
            CallAudioState.ROUTE_WIRED_HEADSET,
            CallAudioState.ROUTE_SPEAKER
        )
            .filter { audioState.supportedRouteMask and it != 0 }
            .map { AudioRoute(name = CallAudioState.audioRouteToString(it), type = it) }

        inCallRepository.onAudioChanged(
            CallAudio(
                isMuted = audioState.isMuted,
                currentRoute = AudioRoute(
                    name = CallAudioState.audioRouteToString(audioState.route),
                    type = audioState.route
                ),
                availableRoutes = availableRoutes
            )
        )
    }

    private enum class NotificationType { INCOMING, OUTGOING, ONGOING }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun CallEndpoint.toAudioRoute(): AudioRoute = AudioRoute(
    name = endpointName.toString(),
    type = when (endpointType) {
        CallEndpoint.TYPE_EARPIECE -> CallAudioState.ROUTE_EARPIECE
        CallEndpoint.TYPE_BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
        CallEndpoint.TYPE_WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
        else -> CallAudioState.ROUTE_SPEAKER // speaker, streaming
    }
)
