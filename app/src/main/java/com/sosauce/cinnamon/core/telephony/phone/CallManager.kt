package com.sosauce.cinnamon.core.telephony.phone

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.repository.SimsRepository
import com.sosauce.cinnamon.core.telecomContactName
import com.sosauce.cinnamon.core.telephony.PhoneNumberNormalizer
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.core.utils.safeStartActivity
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.cinnamon.features.phone.domain.SimChoice
import com.sosauce.cinnamon.features.phone.presentation.call.CallActivity
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import com.sosauce.cinnamon.features.phone.presentation.call.CallingState
import com.sosauce.cinnamon.features.phone.presentation.call.OtherCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

// Inspired by Fossify's call manager!

/**
 * A bridge between an InCallService (CallService) and the ViewModel.
 */
class CallManager(
    private val context: Context,
    private val telecomManager: TelecomManager,
    private val userPreferences: UserPreferences,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val numberLookup: NumberLookup,
    private val simsRepository: SimsRepository
) {

    private var callServiceCallback: CallServiceCallback? = null
    private var androidCallCallback: AndroidCallCallback? = null


    val _callingState = MutableStateFlow(CallingState())


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

    fun holdAndAnswer() = androidCallCallback?.holdAndAnswer()

    fun endAndAnswer() = androidCallCallback?.endAndAnswer()

    fun swapCalls() = androidCallCallback?.swapCalls()

    /**
     * @return Whether the call was placed, and if not, whether it's because the system refused it
     * (see [StartCallResult.NOT_ALLOWED])
     * @param forcedHandle - if provided, uses this PhoneAccountHandle directly (for SIM picker)
     * Returns [StartCallResult.NEEDS_SIM_SELECTION] without placing the call when there are multiple SIMs
     * and no default one, call again with the SIM picked from [fetchSimChoices] as [forcedHandle]
     */
    @SuppressLint("MissingPermission")
    suspend fun startCall(
        number: String,
        forcedHandle: PhoneAccountHandle? = null
    ): StartCallResult {

        return try {
            if (telecomManager.isInCall) return StartCallResult.FAILED
            val phoneAccountHandle = when (val resolution = resolvePhoneAccountHandle(forcedHandle)) {
                is HandleResolution.Use -> resolution.handle
                is HandleResolution.AskUser -> return StartCallResult.NEEDS_SIM_SELECTION
            }

            val bundle = Bundle().apply {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            }

            // USSD/MMI codes (*#06#, *858*...#) must not be formatted as phone numbers
            val isServiceCode = number.any { it == '*' || it == '#' }
            val normalizedNumber = if (isServiceCode) number else phoneNumberNormalizer.formatToE164(number)
            val numberUri = Uri.fromParts(PhoneAccount.SCHEME_TEL, normalizedNumber, null)


            // don't launch the call activity if it's a service code, else we'll just get stuck on it!
            if (isServiceCode) {
                telecomManager.placeCall(numberUri, bundle)
            } else {

                telecomManager.placeCall(numberUri, bundle)

                val callIntent = Intent(context, CallActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                context.safeStartActivity(callIntent)
            }
            StartCallResult.STARTED

        } catch (_: SecurityException) {
            // Phone permissions are granted with the default dialer role
            StartCallResult.NOT_ALLOWED
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            StartCallResult.FAILED
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolvePhoneAccountHandle(forcedHandle: PhoneAccountHandle?): HandleResolution {
        if (forcedHandle != null) return HandleResolution.Use(forcedHandle)

        return try {
            val accounts = telecomManager.callCapablePhoneAccounts.orEmpty()
            if (accounts.size <= 1) {
                return HandleResolution.Use(
                    accounts.firstOrNull()
                        ?: telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
                )
            }

            // Cinnamon's default SIM, or the system one. Null means "Ask every time": the system
            // doesn't show its SIM chooser to the default dialer, so Cinnamon has to ask.
            val savedHandle = userPreferences.getDefaultPhoneHandle().first()
            if (savedHandle != null) HandleResolution.Use(savedHandle) else HandleResolution.AskUser
        } catch (_: SecurityException) {
            HandleResolution.Use(null)
        }
    }

    /**
     * SIMs the user can pick from when [startCall] returns [StartCallResult.NEEDS_SIM_SELECTION]
     */
    fun fetchSimChoices(): List<SimChoice> {
        return simsRepository.fetchPhoneHandles().map { (account, handle) ->
            SimChoice(
                handle = handle,
                label = account.label?.toString().orEmpty(),
                color = account.highlightColor
            )
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

    suspend fun startTone(char: Char) = androidCallCallback?.startTone(char)

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

    fun updateCallProperties(isHdAudio: Boolean, isWifiCall: Boolean) {
        _callingState.update {
            it.copy(isHdAudio = isHdAudio, isWifiCall = isWifiCall)
        }
    }

    fun updateCallState(callState: CallState) {

        if (callState == CallState.ENDED) {
            _callingState.update {
                CallingState(
                    callState = CallState.ENDED
                )
            }
        } else {
            _callingState.update {
                it.copy(callState = callState)
            }
        }
    }

    fun updateTimeSpent(time: Long) {
        _callingState.update {
            it.copy(timeSpentInCall = time)
        }
    }

    /**
     * @param number raw number of the primary call
     */
    fun updateNumber(number: String, details: Call.Details?) {
        val beautified = number.beautifyNumber()
        _callingState.update {
            it.copy(
                number = beautified,
                displayName = numberLookup.fetchCallerName(details, number),
                // Don't keep the previous caller's photo while the new one loads (see updatePhoto)
                photo = if (it.number == beautified) it.photo else null
            )
        }
    }

    /**
     * @param number raw number of the primary call
     */
    suspend fun updatePhoto(number: String, details: Call.Details?) {
        val photo = try {
            numberLookup.fetchCallerPhoto(details, number, true)
        } catch (_: SecurityException) {
            null
        }
        val beautified = number.beautifyNumber()
        _callingState.update {
            // The primary call may have changed while the photo was loading
            if (it.number == beautified) it.copy(photo = photo) else it
        }
    }

    fun updateActiveSim(sim: CuteSimCard) {
        _callingState.update {
            it.copy(activeSim = sim)
        }
    }

    /**
     * @param number raw number of the other call, null when there's no other call
     * @param details details of the other call
     */
    fun updateOtherCall(
        number: String?,
        details: Call.Details?,
        isHolding: Boolean,
        timeSpentInCall: Long
    ) {
        _callingState.update { state ->
            if (number == null) return@update state.copy(otherCall = null)

            val current = state.otherCall
            val beautified = number.beautifyNumber()
            // Only hit the contacts lookup when the other call actually changed,
            // a name Telecom resolved late is still picked up (no query needed for it)
            val displayName =
                if (current != null && current.number == beautified && details?.telecomContactName == null) {
                    current.displayName
                } else {
                    numberLookup.fetchCallerName(details, number)
                }
            OtherCall(
                number = beautified,
                displayName = displayName,
                isHolding = isHolding,
                timeSpentInCall = timeSpentInCall
            ).let { state.copy(otherCall = it) }
        }
    }

    fun updateOtherCallTime(time: Long) {
        _callingState.update {
            it.copy(otherCall = it.otherCall?.copy(timeSpentInCall = time))
        }
    }
}


enum class StartCallResult {
    STARTED,
    FAILED,

    /**
     * The system refused the call with a [SecurityException], Cinnamon needs to be the default dialer app
     */
    NOT_ALLOWED,

    /**
     * The call wasn't placed, the user has to pick a SIM (see [CallManager.fetchSimChoices])
     */
    NEEDS_SIM_SELECTION
}

private sealed interface HandleResolution {
    /**
     * @param handle Null lets Telecom pick the account
     */
    data class Use(val handle: PhoneAccountHandle?) : HandleResolution
    data object AskUser : HandleResolution
}

interface AndroidCallCallback {
    fun answerCall()
    fun declineCall()
    fun holdAndAnswer()
    fun endAndAnswer()
    fun swapCalls()
    fun hangupOngoingCall()
    suspend fun startTone(char: Char)
    fun toggleHold()
}

interface CallServiceCallback {
    fun toggleMute(mute: Boolean)
    fun switchAudioRoute(route: AudioRoute)
}
