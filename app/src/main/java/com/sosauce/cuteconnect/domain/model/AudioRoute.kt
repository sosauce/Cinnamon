package com.sosauce.cuteconnect.domain.model

import android.telecom.CallAudioState

data class AudioRoute(
    val name: String = "No name",
    val type: Int = CallAudioState.ROUTE_EARPIECE

)
