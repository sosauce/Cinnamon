package com.sosauce.cinnamon.core.telephony.phone

import android.content.Context
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.net.toUri
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.telephony.PhoneNumberNormalizer
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import com.sosauce.cinnamon.features.phone.presentation.call.CallingState
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.core.utils.getContactNameOrNothing
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
    private val telecomManager: TelecomManager,
    private val userPreferences: UserPreferences,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
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

    /**
     * @return Whether the call was successfully placed or not
     * @param forcedHandle - if provided, uses this PhoneAccountHandle directly (for SIM picker)
     */
    fun startCall(number: String, forcedHandle: android.telecom.PhoneAccountHandle? = null): Boolean {
        val savedHandle = runBlocking { userPreferences.getDefaultPhoneHandle().first() }
        // Avoid system SIM chooser: if no saved handle and multiple SIMs, pick first capable account
        // This keeps call inside Cinnamon's CallScreen instead of opening system dialer's chooser.
        val fallbackHandle = if (forcedHandle != null) forcedHandle else savedHandle ?: run {
            try {
                @Suppress("MissingPermission")
                telecomManager.callCapablePhoneAccounts?.firstOrNull() as? android.telecom.PhoneAccountHandle
                    ?: telecomManager.getDefaultOutgoingPhoneAccount(android.telecom.PhoneAccount.SCHEME_TEL)
            } catch (_: SecurityException) { null }
        }

        val bundle = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, fallbackHandle)
        }

        val normalizedNumber = phoneNumberNormalizer.formatToE164(number)
        val numberUri = "tel:$normalizedNumber".toUri()

        return try {
            telecomManager.placeCall(numberUri, bundle)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun hangupOngoingCall() {
        val hadCallback = androidCallCallback != null
        androidCallCallback?.hangupOngoingCall()
        // Fallback: if InCallService not yet bound (optimistic UI before onCallAdded),
        // or call was placed but Telecom hasn't delivered, force ENDED so CallActivity finishes.
        // This fixes "end call button not working" when pressed quickly after dialing.
        if (!hadCallback) {
            updateCallState(CallState.ENDED)
        }
        // Also optimistically mark ended — CallService will confirm via STATE_DISCONNECTED
        // but UI should react immediately.
        _callingState.value.let { state ->
            if (state.callState == CallState.DIALING || state.callState == CallState.RINGING || state.callState == CallState.ONGOING) {
                // Don't duplicate if already ENDED, but ensure UI can finish
                // We don't auto-force if already callback succeeded; CallService will update.
                // However for immediate feedback, set to ENDED if callback was null.
            }
        }
    }

    fun forceEndCall() {
        updateCallState(CallState.ENDED)
        androidCallCallback?.hangupOngoingCall()
    }

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

    fun isInCall(): Boolean {
        return try {
            telecomManager.isInCall
        } catch (_: SecurityException) {
            false
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