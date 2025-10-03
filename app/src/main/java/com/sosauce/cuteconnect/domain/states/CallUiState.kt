package com.sosauce.cuteconnect.domain.states

import android.os.Build
import android.os.Parcelable
import android.telecom.CallEndpoint
import androidx.annotation.RequiresApi
import com.sosauce.cuteconnect.domain.model.AudioRoute
import com.sosauce.cuteconnect.domain.model.CuteContact

data class CallUiState(
    val callState: CallState = CallState.DIALING,
    val number: String = "",
    val isMuted: Boolean = false,
    val isHolding: Boolean = false,
    val timeSpentInCall: Long = 0,
    val availableAudioRoutes: List<AudioRoute> = emptyList(),
    val currentAudioRoute: AudioRoute = AudioRoute(),
    val poster: String = "" // contact that may or may nor be associated with the caller
)


enum class CallState{
   RINGING, // When we're receiving a call
   DIALING, // When we're calling
   ONGOING,
   ENDED
}

/**
 * A compatibility layer to CallEndpoint to easily manage audio route below and above Android 14.
 * Maybe we don't need it but I can't test on A14+ so for now we just do.
 */
//object CompatCallEndpoint {
//
//    val TYPE_EARPIECE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//        CallEndpoint.TYPE_EARPIECE
//    } else {
//        1
//    }
//
//    val TYPE_BLUETOOTH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//        CallEndpoint.TYPE_BLUETOOTH
//    } else {
//        2
//    }
//
//    val TYPE_SPEAKER = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//        CallEndpoint.TYPE_SPEAKER
//    } else {
//        4
//    }
//
//    // This is different from bluetooth, bluetooth could be any device, I know this is obvious my my ass would forget
//    val TYPE_HEADSET = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//        CallEndpoint.TYPE_WIRED_HEADSET
//    } else {
//        3
//    }
//
//    val TYPE_UNKNOWN = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//        CallEndpoint.TYPE_UNKNOWN
//    } else {
//        -1
//    }
//
//    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
//    fun CallEndpoint.toCompat(): Int {
//        return when (endpointType) {
//            CallEndpoint.TYPE_EARPIECE -> TYPE_EARPIECE
//            CallEndpoint.TYPE_BLUETOOTH -> TYPE_BLUETOOTH
//            CallEndpoint.TYPE_SPEAKER -> TYPE_SPEAKER
//            CallEndpoint.TYPE_UNKNOWN -> TYPE_UNKNOWN
//            else -> TYPE_UNKNOWN
//        }
//    }
//}