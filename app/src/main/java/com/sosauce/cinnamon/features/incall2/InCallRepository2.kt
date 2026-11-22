package com.sosauce.cinnamon.features.incall2

import android.telecom.Call
import androidx.core.net.toUri
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.utils.copyMutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * Handles already created calls (incoming, outgoing, ingoing). It's NOT responsible for placing calls and notifications
 */
class InCallRepository2(
    private val numberLookup: NumberLookup,
    private val scope: CoroutineScope
) {

    private val _inCallState = MutableStateFlow<InCallState2>(InCallState2.Idle)
    val inCallState = _inCallState.asStateFlow()
    private val callIds = mutableMapOf<Call, String>()
    private fun idFor(call: Call): String = callIds.getOrPut(call) { Uuid.random().toString() }


    val callCallback = object : Call.Callback() {

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)

            if (call == null) return

            scope.launch {
                val info = buildCallInfo(call) ?: return@launch
                _inCallState.update { it.withCall(info) }
            }
        }

        override fun onStateChanged(call: Call?, state: Int) {
            super.onStateChanged(call, state)

            when (state.toCallState()) {
                CallState2.DISCONNECTED -> _inCallState.update { InCallState2.Ended }
                else -> {
                    if (call == null) return

                    scope.launch {
                        val info = buildCallInfo(call) ?: return@launch
                        _inCallState.update { it.withCall(info) }
                    }
                }
            }
        }

    }



    fun onCallAdded(call: Call) {
        call.registerCallback(callCallback)
        scope.launch {
            val info = buildCallInfo(call) ?: return@launch
            _inCallState.update { it.withCall(info) }
        }
    }


    /**
     * Builds a [CallInfo2] for the given call
     */
    private suspend fun buildCallInfo(
        call: Call
    ): CallInfo2? {

        val details = call.details ?: return null
        val number = details.number()
        val contactName =
            details.contactNameApi30() ?: number?.let { numberLookup.fetchContactDisplayName(it) }
        val contactPhoto = details.contactPhotoApi34 ?: number?.let {
            numberLookup.fetchPhoto(it, true)?.toUri()
        }
        val state = call.compatState.toCallState()
        val capabilities = call.toCapabilities()

        return CallInfo2(
            id = idFor(call),
            number = number,
            numberPresentation = details.handlePresentation,
            contactName = contactName,
            contactPhotoUri = contactPhoto,
            state = state,
            connectTimeMillis = details.compatConnectTimeMillis,
            capabilities = capabilities,
            direction = call.toCallDirection(),
            phoneAccountHandle = details.accountHandle,
            parentCallId = call.parent?.let(::idFor),
            childCallIds = call.children.map(::idFor)
        )
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

/**
 * Refreshes an already tracked call (keeping its slot as primary/secondary), or adds
 * a newly arrived call to the end of the list (so basically secondary call).
 */
private fun InCallState2.withCall(info: CallInfo2): InCallState2 = when (this) {
    is InCallState2.Active -> {
        val existingIndex = calls.indexOfFirst { it.id == info.id }
        copy(
            calls = calls.copyMutate {
                if (existingIndex >= 0) {
                    this[existingIndex] = info
                } else {
                    add(info)
                }
            }
        )
    }

    else -> InCallState2.Active(calls = listOf(info))
}