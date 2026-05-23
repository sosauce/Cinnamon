package com.sosauce.cinnamon.data.managers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import com.sosauce.cinnamon.data.datastore.UserPreferences
import com.sosauce.cinnamon.domain.model.AudioRoute
import com.sosauce.cinnamon.domain.model.CuteSimCard
import com.sosauce.cinnamon.domain.states.CallState
import com.sosauce.cinnamon.presentation.screens.phone.CallingState
import com.sosauce.cinnamon.utils.beautifyNumber
import com.sosauce.cinnamon.utils.getContactNameOrNothing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

// Inspired by Fossify's call manager!

/**
 * A bridge between an InCallService (CallService) and the ViewModel.
 */
class CallManager(
    private val context: Context,
    val telecomManager: TelecomManager,
    private val userPreferences: UserPreferences
) {

    private var callServiceCallback: CallServiceCallback? = null
    private var androidCallCallback: AndroidCallCallback? = null


    val _callingState = MutableStateFlow(CallingState())
    val callingState = _callingState.asStateFlow()


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

    @SuppressLint("MissingPermission")
    fun startCall(number: Uri) {

        val phoneHandle = runBlocking { userPreferences.getDefaultPhoneHandle().first() }

        val bundle = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneHandle)
        }

        telecomManager.placeCall(number, bundle)
    }


    fun hangupOngoingCall() = androidCallCallback?.hangupOngoingCall()

    fun toggleMute(mute: Boolean) = callServiceCallback?.toggleMute(mute)

    fun startTone(char: Char) = androidCallCallback?.startTone(char)

    fun toggleHold() = androidCallCallback?.toggleHold()

    fun switchAudioRoute(route: AudioRoute) = callServiceCallback?.switchAudioRoute(route)

    fun updateAvailableAudioRoutes(routes: List<AudioRoute>) {
        _callingState.update {
            it.copy(availableAudioRoutes = routes)
        }
    }

    fun updateCurrentAudioRoute(route: AudioRoute) {
        _callingState.update {
            it.copy(currentAudioRoute = route)
        }
    }

    fun updateIsMuted(isMuted: Boolean) {
        _callingState.update {
            it.copy(isMuted = isMuted)
        }
    }

    fun updateIsHolding(isHolding: Boolean) {
        _callingState.update {
            it.copy(isHolding = isHolding)
        }
    }

    fun updateCallState(callState: CallState) {
        _callingState.update {
            it.copy(callState = callState)
        }
    }

    fun updateTimeSpent(time: Long) {
        _callingState.update {
            it.copy(timeSpentInCall = time)
        }
    }

    fun updateNumber(number: String) {
        _callingState.update {
            it.copy(
                number = number,
                displayName = number.getContactNameOrNothing(context).beautifyNumber()
            )
        }
    }

    fun updateActiveSim(sim: CuteSimCard) {
        _callingState.update {
            it.copy(activeSim = sim)
        }
    }
}


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