package com.sosauce.cinnamon.features.incall

import android.telecom.CallAudioState


data class AudioRoute(
    val name: String = "No name",
    val type: Int = CallAudioState.ROUTE_EARPIECE

)

data class CallAudio(
    val isMuted: Boolean = false,
    val currentRoute: AudioRoute = AudioRoute(),
    val availableRoutes: List<AudioRoute> = emptyList()
)

interface CallAudioController {
    fun mute(muted: Boolean)
    fun switchAudioRoute(route: AudioRoute)
}
