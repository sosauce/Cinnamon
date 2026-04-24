package com.sosauce.cinnamon.presentation.screens.phone.components

import android.telecom.CallAudioState
import com.sosauce.cinnamon.R

fun Int.routeToIcon(): Int {
    return when (this) {
        CallAudioState.ROUTE_EARPIECE -> R.drawable.earpiece
        CallAudioState.ROUTE_SPEAKER -> R.drawable.speaker
        CallAudioState.ROUTE_WIRED_HEADSET -> R.drawable.headset
        CallAudioState.ROUTE_BLUETOOTH -> R.drawable.bluetooth_call
        else -> R.drawable.speaker
    }
}