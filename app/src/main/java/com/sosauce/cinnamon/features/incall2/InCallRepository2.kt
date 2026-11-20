package com.sosauce.cinnamon.features.incall2

import android.telecom.Call
import androidx.compose.ui.util.fastFirst
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles already created calls. Is NOT responsible for 
 */
class InCallRepository2 {

    private val _inCallState = MutableStateFlow(InCallState2.Idle)
    val inCallState = _inCallState.asStateFlow()


    fun onCallAdded(call: Call) {

    }


}



sealed interface InCallState2 {

    data class Active(
        val calls: List<CallInfo2> = emptyList(),
        //val audio: CallAudioState2
    ) : InCallState2 {
        val primaryCall
            get() = calls.getOrNull(0)
        val secondaryCall
            get() = calls.getOrNull(1)
    }

    data object Idle : InCallState2
    data object Ended : InCallState2
}