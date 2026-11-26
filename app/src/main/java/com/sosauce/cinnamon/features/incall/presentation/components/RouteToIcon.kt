package com.sosauce.cinnamon.features.incall.presentation.components

import android.telecom.CallAudioState
import com.sosauce.cinnamon.R

fun Int.routeToIcon(filled: Boolean = false): Int {
    return when (this) {
        CallAudioState.ROUTE_EARPIECE -> if (filled) R.drawable.earpiece_filled else R.drawable.earpiece
        CallAudioState.ROUTE_SPEAKER -> if (filled) R.drawable.speaker_filled else R.drawable.speaker
        CallAudioState.ROUTE_WIRED_HEADSET -> if (filled) R.drawable.headset_filled else R.drawable.headset
        CallAudioState.ROUTE_BLUETOOTH -> R.drawable.bluetooth_call
        else -> if (filled) R.drawable.speaker_filled else R.drawable.speaker
    }
}