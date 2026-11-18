@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call

import android.app.Application
import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.core.telephony.phone.StartCallResult
import com.sosauce.cinnamon.core.utils.getContactId
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.cinnamon.features.phone.domain.SimChoice
import com.sosauce.cinnamon.features.phone.domain.SimPickerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallingViewModel(
    private val application: Application,
    private val callManager: CallManager,
    private val contactSettingsDao: ContactSettingsDao,
    private val numberLookup: NumberLookup
) : AndroidViewModel(application) {


    private val _state = callManager._callingState
    val state = _state.asStateFlow()

    private val _events = Channel<CallEvents>()
    val events = _events.receiveAsFlow()

    // Not in CallingState, which is shared by every screen through CallManager
    private val _simPicker = MutableStateFlow<SimPickerState?>(null)
    val simPicker = _simPicker.asStateFlow()
    private var pickedSimCallJob: Job? = null


    init {
        viewModelScope.launch(Dispatchers.IO) {
            state.mapLatest { it.number }.distinctUntilChanged().collectLatest { number ->
                // contacts can't be read without the default dialer role
                val (photo, poster) = try {
                    val contactId = number.getContactId(application.applicationContext)

                    numberLookup.fetchPhoto(number, true) to
                            contactSettingsDao.getContactPoster(contactId)
                } catch (_: SecurityException) {
                    null to null
                }
                _state.update {
                    it.copy(
                        photo = photo?.toUri(),
                        poster = poster?.toUri()
                    )
                }
            }
        }
    }

    fun handleCallAction(action: CallAction) {
        when (action) {
            is CallAction.LaunchCall -> {
                viewModelScope.launch { launchCall(action.number) }
            }

            is CallAction.PickSim -> {
                val number = _simPicker.value?.number ?: return
                if (pickedSimCallJob?.isActive == true) return
                pickedSimCallJob = viewModelScope.launch {
                    // the picker is closed once the call state is updated, so CallActivity doesn't
                    // finish in between because of the previous call's ENDED state
                    launchCall(number, action.sim.handle)
                    _simPicker.update { null }
                }
            }

            is CallAction.DismissSimPicker -> _simPicker.update { null }

            is CallAction.AnswerCall -> callManager.answerCall()
            is CallAction.DeclineCall -> callManager.declineCall()
            is CallAction.HangUp -> callManager.hangupOngoingCall()
            is CallAction.StartTone -> {
                viewModelScope.launch {
                    callManager.startTone(action.char)
                }
            }

            is CallAction.SwitchAudioTarget -> callManager.switchAudioRoute(action.route)
            is CallAction.ToggleHold -> callManager.toggleHold()
            is CallAction.ToggleMute -> callManager.toggleMute(action.mute)
            is CallAction.HoldAndAnswer -> callManager.holdAndAnswer()
            is CallAction.EndAndAnswer -> callManager.endAndAnswer()
            is CallAction.SwapCalls -> callManager.swapCalls()
        }
    }

    private suspend fun launchCall(number: String, handle: PhoneAccountHandle? = null) {
        when (callManager.startCall(number, handle)) {
            StartCallResult.NOT_ALLOWED -> {
                _events.send(CallEvents.RequestDefaultDialer)
                return
            }

            StartCallResult.NEEDS_SIM_SELECTION -> {
                _simPicker.value = SimPickerState(number, callManager.fetchSimChoices())
                return
            }

            else -> Unit
        }
        // Optimistically update state and launch UI even if placeCall
        // threw, ensures call button always opens Cinnamon's CallScreen
        // (incoming UI is handled separately via CallService fullScreenIntent).
        callManager._callingState.update {
            it.copy(
                number = number,
                displayName = number,
                callState = CallState.DIALING
            )
        }
    }

}


/**
 * @param activeSim Sim used for the ongoing call, for incoming calls for example, it's the sim that's getting called + is gonna get used for the call
 */
@Stable
data class CallingState(
    val callState: CallState = CallState.DIALING,
    val number: String = "",
    val displayName: String = "",
    val isMuted: Boolean = false,
    val isHolding: Boolean = false,
    val timeSpentInCall: Long = 0,
    val availableAudioRoutes: List<AudioRoute> = emptyList(),
    val currentAudioRoute: AudioRoute = AudioRoute(),
    val photo: Uri? = null,
    val poster: Uri? = null, // contact that may or may nor be associated with the caller
    val activeSim: CuteSimCard = CuteSimCard(),
    val otherCall: OtherCall? = null // second call (call waiting / on hold), null when there's only one call

)

/**
 * The call that isn't shown as the main one in the call screen.
 */
@Stable
data class OtherCall(
    val number: String = "",
    val displayName: String = "",
    val isHolding: Boolean = false,
    val timeSpentInCall: Long = 0
)

sealed interface CallAction {
    data class LaunchCall(val number: String) : CallAction
    data class PickSim(val sim: SimChoice) : CallAction
    data object DismissSimPicker : CallAction
    data class StartTone(val char: Char) : CallAction
    data class ToggleMute(val mute: Boolean) : CallAction
    data class SwitchAudioTarget(val route: AudioRoute) : CallAction
    data object AnswerCall : CallAction
    data object DeclineCall : CallAction
    data object ToggleHold : CallAction
    data object HangUp : CallAction
    data object HoldAndAnswer : CallAction
    data object EndAndAnswer : CallAction
    data object SwapCalls : CallAction
}

sealed interface CallEvents {
    /**
     * The call couldn't be launched because Cinnamon isn't the default dialer app
     */
    data object RequestDefaultDialer : CallEvents
}
