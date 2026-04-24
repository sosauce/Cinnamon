package com.sosauce.cinnamon.presentation.screens.phone

import android.annotation.SuppressLint
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.sosauce.cinnamon.data.managers.CallManager
import com.sosauce.cinnamon.domain.model.AudioRoute
import com.sosauce.cinnamon.domain.model.CuteSimCard
import com.sosauce.cinnamon.domain.states.CallState
import kotlinx.coroutines.flow.asStateFlow

class CallingViewModel(
    private val callManager: CallManager
): ViewModel() {


    val state = callManager.callingState

    @SuppressLint("MissingPermission")
    fun handleCallAction(action: CallAction) {
        when(action) {
            is CallAction.LaunchCall -> {
                val numberUri = "tel:${action.number}".toUri()

                if (!callManager.telecomManager.isInCall) {
                    callManager.startCall(numberUri)
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
    val poster: String = "", // contact that may or may nor be associated with the caller
    val activeSim: CuteSimCard = CuteSimCard()

)

sealed interface CallAction {
    data class LaunchCall(val number: String) : CallAction
    data class StartTone(val char: Char) : CallAction
    data class ToggleMute(val mute: Boolean) : CallAction
    data class SwitchAudioTarget(val route: AudioRoute) : CallAction
    data object AnswerCall: CallAction
    data object DeclineCall: CallAction
    data object ToggleHold: CallAction
    data object HangUp: CallAction
}