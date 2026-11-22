@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.incall.presentation

import android.app.Application
import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.utils.getContactId
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.incall.AudioRoute
import com.sosauce.cinnamon.features.incall.CallLauncher
import com.sosauce.cinnamon.features.incall.InCallRepository
import com.sosauce.cinnamon.features.incall.InCallState
import com.sosauce.cinnamon.features.incall.LaunchCallResult
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.cinnamon.features.phone.domain.SimChoice
import com.sosauce.cinnamon.features.phone.domain.SimPickerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallingViewModel(
    private val application: Application,
    private val inCallRepository: InCallRepository,
    private val callLauncher: CallLauncher,
    private val callingStateMapper: CallingStateMapper,
    private val contactSettingsDao: ContactSettingsDao
) : AndroidViewModel(application) {

    // Poster of the contact that may be associated with the primary call's number
    private val poster = callingStateMapper.callingState
        .map { it.number }
        .distinctUntilChanged()
        .mapLatest { number ->
            withContext(Dispatchers.IO) {
                try {
                    val contactId = number.getContactId(application.applicationContext)
                    contactSettingsDao.getContactPoster(contactId)?.toUri()
                } catch (_: SecurityException) {
                    null
                }
            }
        }
        .onStart { emit(null) }

    val state = combine(callingStateMapper.callingState, poster) { state, poster ->
        state.copy(poster = poster)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        callingStateMapper.currentCallingState()
    )

    private val _events = Channel<CallEvents>()
    val events = _events.receiveAsFlow()

    // Only for calls launched by CallActivity (ACTION_CALL intents)
    private val _simPicker = MutableStateFlow<SimPickerState?>(null)
    val simPicker = _simPicker.asStateFlow()
    private var pickedSimCallJob: Job? = null

    private val activeState
        get() = inCallRepository.inCallState.value as? InCallState.Active


    fun handleCallAction(action: CallAction) {
        when (action) {
            is CallAction.LaunchCall -> {
                viewModelScope.launch { launchCall(action.number) }
            }

            is CallAction.PickSim -> {
                val number = _simPicker.value?.number ?: return
                if (pickedSimCallJob?.isActive == true) return
                pickedSimCallJob = viewModelScope.launch {
                    launchCall(number, action.sim.handle)
                    _simPicker.update { null }
                }
            }

            is CallAction.DismissSimPicker -> _simPicker.update { null }

            is CallAction.AnswerCall -> activeState?.ringingCall?.let { inCallRepository.answer(it.id) }
            is CallAction.DeclineCall -> activeState?.ringingCall?.let { inCallRepository.reject(it.id) }
            is CallAction.HangUp -> activeState?.let { inCallRepository.disconnect(it.primaryCall.id) }
            is CallAction.StartTone -> {
                val id = activeState?.primaryCall?.id ?: return
                viewModelScope.launch {
                    inCallRepository.startTone(id, action.char)
                }
            }

            is CallAction.SwitchAudioTarget -> inCallRepository.setAudioRoute(action.route)
            is CallAction.ToggleHold -> activeState?.let { inCallRepository.toggleHold(it.primaryCall.id) }
            is CallAction.ToggleMute -> inCallRepository.setMuted(action.mute)
            // Telecom holds the current active call itself when answering a waiting call
            is CallAction.HoldAndAnswer -> activeState?.ringingCall?.let { inCallRepository.answer(it.id) }
            is CallAction.EndAndAnswer -> activeState?.ringingCall?.let { inCallRepository.endAndAnswer(it.id) }
            is CallAction.SwapCalls -> inCallRepository.swap()
        }
    }

    private suspend fun launchCall(number: String, handle: PhoneAccountHandle? = null) {
        when (val result = callLauncher.launchCall(number, handle)) {
            LaunchCallResult.NotAllowed -> _events.send(CallEvents.RequestDefaultDialer)
            is LaunchCallResult.NeedsSimSelection -> _simPicker.value = SimPickerState(number, result.choices)
            // CallService shows the call once Telecom adds it
            LaunchCallResult.Started -> Unit
            // Nothing to show
            LaunchCallResult.ServiceCodeSent, LaunchCallResult.Failed -> _events.send(CallEvents.NoCallToShow)
        }
    }

}


/**
 * @param activeSim Sim used for the ongoing call, for incoming calls for example, it's the sim that's getting called + is gonna get used for the call
 */
@Stable
data class CallingState(
    val callState: CallUiState = CallUiState.DIALING,
    val number: String = "",
    val displayName: String? = null,
    val isMuted: Boolean = false,
    val isHolding: Boolean = false,
    val timeSpentInCall: Long = 0,
    val availableAudioRoutes: List<AudioRoute> = emptyList(),
    val currentAudioRoute: AudioRoute = AudioRoute(),
    val photo: Uri? = null,
    val poster: Uri? = null, // contact that may or may nor be associated with the caller
    val activeSim: CuteSimCard = CuteSimCard(),
    val isHdAudio: Boolean = false,
    val isWifiCall: Boolean = false,
    val otherCall: OtherCall? = null // second call (call waiting / on hold), null when there's only one call

)

/**
 * The call that isn't shown as the main one in the call screen.
 */
@Stable
data class OtherCall(
    val number: String = "",
    val displayName: String? = null,
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

    /**
     * The launched number didn't become a call (service code, failure), CallActivity has nothing to show
     */
    data object NoCallToShow : CallEvents
}
