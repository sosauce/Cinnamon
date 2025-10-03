package com.sosauce.cuteconnect.ui.screens.phone.components

import android.telecom.CallAudioState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sosauce.cuteconnect.domain.states.CallUiState

fun Int.routeToIcon(): ImageVector {
    return when (this) {
        CallAudioState.ROUTE_EARPIECE -> Icons.AutoMirrored.Rounded.VolumeDown
        CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Rounded.VolumeUp
        CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Rounded.Headset
        CallAudioState.ROUTE_BLUETOOTH -> Icons.Rounded.BluetoothAudio
        else -> Icons.AutoMirrored.Rounded.VolumeDown
    }
}