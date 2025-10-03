package com.sosauce.cuteconnect.data.actions

import com.sosauce.cuteconnect.domain.model.AudioRoute

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