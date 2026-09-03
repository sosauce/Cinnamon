package com.sosauce.cinnamon.features.phone.presentation.call

import android.app.Application
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.cinnamon.core.utils.getContactId
import com.sosauce.cinnamon.features.phone.presentation.call.CallActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallingViewModel(
    private val application: Application,
    private val callManager: CallManager,
    private val contactSettingsDao: ContactSettingsDao
) : AndroidViewModel(application) {


    val state = callManager.callingState


    init {
        viewModelScope.launch(Dispatchers.IO) {

            val poster =
                contactSettingsDao.getContactPoster(state.value.number.getContactId(application.applicationContext))
                    ?: ""

            callManager._callingState.update {
                it.copy(
                    poster = poster
                )
            }


        }
    }

    fun handleCallAction(action: CallAction) {
        when (action) {
            is CallAction.LaunchCall -> {
                if (callManager.isInCall()) return
                // Always open Cinnamon's call UI — no default-dialer gating.
                // Telecom will still place the call via CallManager; we show our
                // expressive CallScreen immediately for instant feedback.
                val success = try {
                    callManager.startCall(action.number)
                } catch (_: Exception) { false }
                // Optimistically update state and launch UI even if placeCall
                // threw — ensures call button always opens Cinnamon's CallScreen
                // (incoming UI is handled separately via CallService fullScreenIntent).
                callManager._callingState.update {
                    it.copy(
                        number = action.number,
                        displayName = action.number,
                        callState = CallState.DIALING
                    )
                }
                try {
                    val intent = Intent(application, CallActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    application.startActivity(intent)
                } catch (_: Exception) {}
                // `success` is kept for future retry logic if needed, but UI always opens.
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
    data object AnswerCall : CallAction
    data object DeclineCall : CallAction
    data object ToggleHold : CallAction
    data object HangUp : CallAction
}

sealed interface CallEvents {

}