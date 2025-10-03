package com.sosauce.cuteconnect.data.managers

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.Connection
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.TelephonyCallback
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.activities.CallActivity
import com.sosauce.cuteconnect.data.receivers.CallReceiver
import com.sosauce.cuteconnect.data.services.CallService
import com.sosauce.cuteconnect.domain.model.AudioRoute
import com.sosauce.cuteconnect.domain.states.CallState
import com.sosauce.cuteconnect.domain.states.CallUiState
import com.sosauce.cuteconnect.main.MainActivity
import com.sosauce.cuteconnect.utils.ACCEPT_INCOMING_CALL
import com.sosauce.cuteconnect.utils.AUDIO_SOURCE
import com.sosauce.cuteconnect.utils.AudioTargetDevice
import com.sosauce.cuteconnect.utils.DECLINE_INCOMING_CALL
import com.sosauce.cuteconnect.utils.FULL_SCREEN_INTENT
import com.sosauce.cuteconnect.utils.HANGUP_ONGOING_CALL
import com.sosauce.cuteconnect.utils.MUTE_SOURCE
import com.sosauce.cuteconnect.utils.SWITCH_AUDIO_SOURCE
import com.sosauce.cuteconnect.utils.getContactNameOrNothing
import com.sosauce.cuteconnect.viewModels.CallViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

// Inspired by Fossify's call manager!

/**
 * A bridge between an InCallService (CallService) and the ViewModel.
 * It's easier having it as an object as InCallService has a hard time with DI
 */
object CallManager {

    private var callServiceCallback: CallServiceCallback? = null
    private var androidCallCallback : AndroidCallCallback? = null


    private val _callUIState = MutableStateFlow(CallUiState())
    val callUiState = _callUIState.asStateFlow()

    fun registerCallServiceCallback(cb: CallServiceCallback) {
        callServiceCallback = cb
    }

    fun registerAndroidCallCallback(cb: AndroidCallCallback) {
        androidCallCallback = cb
    }

    fun unregisterCallServiceCallback() {
        callServiceCallback = null
    }

    fun unregisterAndroidCallCallback() {
        androidCallCallback = null
    }

    fun answerCall() = androidCallCallback?.answerCall()

    fun declineCall() = androidCallCallback?.declineCall()


    fun hangupOngoingCall() = androidCallCallback?.hangupOngoingCall()

    fun toggleMute(mute: Boolean) = callServiceCallback?.toggleMute(mute)

    fun startTone(char: Char) = androidCallCallback?.startTone(char)

    fun toggleHold() = androidCallCallback?.toggleHold()

    fun switchAudioRoute(route: AudioRoute) = callServiceCallback?.switchAudioRoute(route)

    fun updateAvailableAudioRoutes(routes: List<AudioRoute>) {
        _callUIState.update {
            it.copy(availableAudioRoutes = routes)
        }
    }

    fun updateCurrentAudioRoute(route: AudioRoute) {
        _callUIState.update {
            it.copy(currentAudioRoute = route)
        }
    }

    fun updateIsMuted(isMuted: Boolean) {
        _callUIState.update {
            it.copy(isMuted = isMuted)
        }
    }

    fun updateIsHolding(isHolding: Boolean) {
        _callUIState.update {
            it.copy(isHolding = isHolding)
        }
    }

    fun updateCallState(callState: CallState) {
        _callUIState.update {
            it.copy(callState = callState)
        }
    }

    fun updateTimeSpent(time: Long) {
        _callUIState.update {
            it.copy(timeSpentInCall = time)
        }
    }

    fun updateNumber(number: String) {
        _callUIState.update {
            it.copy(number = number)
        }
    }
}


// Idk if this is best practice, but I'd rather have as less references as possible in the CallManager since its a singleton and it can easily leak
interface AndroidCallCallback {
    fun answerCall()
    fun declineCall()
    fun hangupOngoingCall()
    fun startTone(char: Char)
    fun toggleHold()
}
interface CallServiceCallback {
    fun toggleMute(mute: Boolean)
    fun switchAudioRoute(route: AudioRoute)
}