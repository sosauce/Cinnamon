@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.core.utils.getContactId
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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


    init {
        viewModelScope.launch(Dispatchers.IO) {

            val id = state.value.number.getContactId(application.applicationContext)
            val poster = contactSettingsDao.getContactPoster(id)
            callManager._callingState.update {
                it.copy(
                    poster = poster?.toUri()
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            state.mapLatest { it.number }.distinctUntilChanged().collectLatest { number ->
                _state.update {
                    it.copy(
                        photo = numberLookup.fetchPhoto(number, true)?.toUri()
                    )
                }
            }
        }
    }

    fun handleCallAction(action: CallAction) {
        when (action) {
            is CallAction.LaunchCall -> {
                callManager.startCall(action.number)
                // Optimistically update state and launch UI even if placeCall
                // threw, ensures call button always opens Cinnamon's CallScreen
                // (incoming UI is handled separately via CallService fullScreenIntent).
                callManager._callingState.update {
                    it.copy(
                        number = action.number,
                        displayName = action.number,
                        callState = CallState.DIALING
                    )
                }
            }

            is CallAction.AnswerCall -> callManager.answerCall()
            is CallAction.DeclineCall -> callManager.declineCall()
            is CallAction.HangUp -> callManager.hangupOngoingCall()
            is CallAction.StartTone -> callManager.startTone(action.char)
            is CallAction.SwitchAudioTarget -> callManager.switchAudioRoute(action.route)
            is CallAction.ToggleHold -> callManager.toggleHold()
            is CallAction.ToggleMute -> callManager.toggleMute(action.mute)
        }
    }

}


/**
 * @param activeSim Sim used for the ongoing call, for incoming calls for example, it's the sim that's getting called + is gonna get used for the call
 */
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
    val activeSim: CuteSimCard = CuteSimCard()

)

sealed interface CallAction {
    data class LaunchCall(val number: String) : CallAction
    data class StartTone(val char: Char) : CallAction
    data class ToggleMute(val mute: Boolean) : CallAction
    data class SwitchAudioTarget(val route: AudioRoute) : CallAction
    data object AnswerCall : CallAction
    data object DeclineCall : CallAction
    data object ToggleHold : CallAction
    data object HangUp : CallAction
}

sealed interface CallEvents {

}