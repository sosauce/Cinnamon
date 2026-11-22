package com.sosauce.cinnamon.features.incall

import android.net.Uri
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.core.net.toUri
import com.sosauce.cinnamon.core.NumberLookup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

/**
 * Holds the state of already created calls (incoming, outgoing, ongoing) and runs actions on them.
 * It's NOT responsible for placing calls (see [CallLauncher]), notifications or launching the call UI (see [CallService]).
 *
 * Everything runs on the main thread (Telecom callbacks, UI actions and the injected scope).
 */
class InCallRepository(
    private val numberLookup: NumberLookup,
    private val scope: CoroutineScope
) {

    private val _inCallState = MutableStateFlow<InCallState>(InCallState.Idle)
    val inCallState = _inCallState.asStateFlow()

    private val _callAudio = MutableStateFlow(CallAudio())
    val callAudio = _callAudio.asStateFlow()

    private val callsById = mutableMapOf<String, Call>()
    private val callIds = mutableMapOf<Call, String>()

    // Contact name/photo found for each call's number, when Telecom doesn't provide them
    private val contacts = mutableMapOf<String, ContactLookup>()
    private val lookupJobs = mutableMapOf<String, Job>()

    // Fallback start time for calls whose details don't expose connectTimeMillis
    private val fallbackConnectTimes = mutableMapOf<String, Long>()

    // "End & answer": answer this ringing call once the calls we ended are gone
    private var pendingAnswerId: String? = null
    private var endedForAnswer: Set<String> = emptySet()
    private var pendingAnswerTimeout: Job? = null

    private var audioController: CallAudioController? = null

    /**
     * @return The id given to [call]
     */
    fun onCallAdded(call: Call): String {
        val id = Uuid.random().toString()
        callIds[call] = id
        callsById[id] = call
        refresh(call)
        return id
    }

    fun onCallUpdated(call: Call) {
        if (call !in callIds) return
        refresh(call)
    }

    fun onCallRemoved(call: Call) {
        val id = callIds.remove(call) ?: return
        callsById.remove(id)
        contacts.remove(id)
        lookupJobs.remove(id)?.cancel()
        fallbackConnectTimes.remove(id)
        _inCallState.update { it.withoutCall(id) }
        if (callsById.isEmpty()) {
            _callAudio.value = CallAudio()
            clearPendingAnswer()
        } else {
            handlePendingAnswer()
        }
    }

    fun setAudioController(controller: CallAudioController?) {
        audioController = controller
    }

    fun onAudioChanged(audio: CallAudio) {
        _callAudio.value = audio
    }

    /**
     * The Telecom call behind [id], for things that need the platform object (notifications...)
     */
    fun callFor(id: String): Call? = callsById[id]

    // --- Actions ---

    fun answer(id: String) {
        callsById[id]?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject(id: String) {
        callsById[id]?.reject(false, null)
    }

    fun disconnect(id: String) {
        callsById[id]?.disconnect()
    }

    fun toggleHold(id: String) {
        val call = callsById[id] ?: return
        if (call.compatState == Call.STATE_HOLDING) call.unhold() else call.hold()
    }

    /**
     * Unholding the held call makes Telecom hold the currently active one
     */
    fun swap() {
        val calls = (_inCallState.value as? InCallState.Active)?.calls ?: return
        val primary = calls.firstOrNull()
        val held = calls.firstOrNull { it.id != primary?.id && it.state == CallState.HOLDING }
            ?: primary?.takeIf { it.state == CallState.HOLDING }
        held?.let { callsById[it.id]?.unhold() }
    }

    /**
     * Ends the active call (a call already on hold stays on hold), then answers [ringingId]
     */
    fun endAndAnswer(ringingId: String) {
        val calls = (_inCallState.value as? InCallState.Active)?.calls ?: return
        val others = calls.filter { it.id != ringingId }
        val toEnd = others.filter { it.state == CallState.ACTIVE }.ifEmpty { others }

        if (toEnd.isEmpty()) {
            answer(ringingId)
            return
        }

        // Answer once the ended calls are gone (see handlePendingAnswer), otherwise Telecom
        // would try to hold the call we're disconnecting.
        pendingAnswerId = ringingId
        endedForAnswer = toEnd.map { it.id }.toSet()
        pendingAnswerTimeout?.cancel()
        pendingAnswerTimeout = scope.launch {
            delay(PENDING_ANSWER_TIMEOUT_MILLIS.milliseconds)
            val pending = pendingAnswerId ?: return@launch
            clearPendingAnswer()
            if (callsById[pending]?.compatState == Call.STATE_RINGING) answer(pending)
        }
        toEnd.forEach { disconnect(it.id) }
    }

    suspend fun startTone(id: String, char: Char) {
        val call = callsById[id] ?: return
        call.playDtmfTone(char)
        delay(DTMF_TONE_DURATION_MILLIS.milliseconds)
        call.stopDtmfTone()
    }

    fun setMuted(muted: Boolean) {
        audioController?.mute(muted)
    }

    fun setAudioRoute(route: AudioRoute) {
        audioController?.switchAudioRoute(route)
    }

    // --- Internals ---

    private fun refresh(call: Call) {
        val id = callIds[call] ?: return
        val info = buildCallInfo(call, id) ?: return
        _inCallState.update { it.withCall(info) }
        handlePendingAnswer()
        lookupContact(id, info)
    }

    /**
     * Builds a [CallInfo] for the given call, synchronously so updates keep Telecom's order.
     * The contact name/photo come from Telecom, or from [lookupContact] once it's done.
     */
    private fun buildCallInfo(call: Call, id: String): CallInfo? {
        val details = call.details ?: return null
        val number = details.number()
        val state = call.compatState.toCallState()
        val contact = contacts[id]?.takeIf { it.number == number }

        return CallInfo(
            id = id,
            number = number,
            numberPresentation = details.handlePresentation,
            contactName = details.contactNameApi30() ?: contact?.name,
            contactPhotoUri = details.contactPhotoApi34 ?: contact?.photo,
            state = state,
            connectTimeMillis = connectTimeFor(id, details, state),
            capabilities = call.toCapabilities(),
            direction = call.toCallDirection(),
            phoneAccountHandle = details.accountHandle,
            parentCallId = call.parent?.let { callIds[it] },
            childCallIds = call.children.mapNotNull { callIds[it] }
        )
    }

    private fun connectTimeFor(id: String, details: Call.Details, state: CallState): Long? {
        details.connectTimeMillis.takeIf { it > 0 }?.let { return it }
        return if (state == CallState.ACTIVE || state == CallState.HOLDING) {
            fallbackConnectTimes.getOrPut(id) { System.currentTimeMillis() }
        } else null
    }

    /**
     * Looks up the contact behind the call's number when Telecom doesn't provide its name or photo
     */
    private fun lookupContact(id: String, info: CallInfo) {
        val number = info.number ?: return
        if (info.contactName != null && info.contactPhotoUri != null) return
        if (contacts[id]?.number == number) return // already looked up (or looking up) this number

        contacts[id] = ContactLookup(number, null, null)
        lookupJobs[id]?.cancel()
        lookupJobs[id] = scope.launch {
            val name = withContext(Dispatchers.IO) {
                try {
                    numberLookup.fetchContactDisplayName(number)
                } catch (_: SecurityException) {
                    null
                }
            }
            val photo = try {
                numberLookup.fetchPhoto(number, true)?.toUri()
            } catch (_: SecurityException) {
                null
            }
            // The call's number may have changed (or the call ended) meanwhile
            if (contacts[id]?.number != number) return@launch
            contacts[id] = ContactLookup(number, name, photo)
            _inCallState.update { state ->
                state.updateCall(id) {
                    it.copy(
                        contactName = it.contactName ?: name,
                        contactPhotoUri = it.contactPhotoUri ?: photo
                    )
                }
            }
        }
    }

    private fun handlePendingAnswer() {
        val pending = pendingAnswerId ?: return
        if (callsById[pending]?.compatState != Call.STATE_RINGING) {
            clearPendingAnswer()
            return
        }
        val live = (_inCallState.value as? InCallState.Active)?.calls.orEmpty().map { it.id }
        if (live.none { it in endedForAnswer }) {
            clearPendingAnswer()
            answer(pending)
        }
    }

    private fun clearPendingAnswer() {
        pendingAnswerId = null
        endedForAnswer = emptySet()
        pendingAnswerTimeout?.cancel()
        pendingAnswerTimeout = null
    }

    private data class ContactLookup(val number: String, val name: String?, val photo: Uri?)

    companion object {
        private const val DTMF_TONE_DURATION_MILLIS = 150
        private const val PENDING_ANSWER_TIMEOUT_MILLIS = 3000
    }
}


sealed interface InCallState {

    /**
     * @param calls Live calls (not disconnecting/disconnected), most important first, never empty
     */
    data class Active(
        val calls: List<CallInfo>
    ) : InCallState {
        val primaryCall
            get() = calls.first()
        val secondaryCall
            get() = calls.getOrNull(1)
        val ringingCall
            get() = calls.firstOrNull { it.state == CallState.RINGING }
    }

    data object Idle : InCallState
    data object Ended : InCallState
}

/**
 * Refreshes an already tracked call or adds a newly arrived one, then keeps the most important
 * calls first: ringing > active > dialing > held. Calls with the same priority keep their order.
 * A disconnecting/disconnected call leaves the list, and the state is [InCallState.Ended] once it's empty.
 */
private fun InCallState.withCall(info: CallInfo): InCallState {
    val current = (this as? InCallState.Active)?.calls.orEmpty()
    val existingIndex = current.indexOfFirst { it.id == info.id }
    val updated = if (existingIndex >= 0) {
        current.toMutableList().apply { this[existingIndex] = info }
    } else {
        current + info
    }
    return updated.toState(fallback = this)
}

private fun InCallState.withoutCall(id: String): InCallState {
    val current = (this as? InCallState.Active)?.calls ?: return this
    return current.filterNot { it.id == id }.toState(fallback = InCallState.Ended)
}

private fun InCallState.updateCall(id: String, transform: (CallInfo) -> CallInfo): InCallState {
    if (this !is InCallState.Active) return this
    return copy(calls = calls.map { if (it.id == id) transform(it) else it })
}

private fun List<CallInfo>.toState(fallback: InCallState): InCallState {
    val live = mapNotNull { call -> call.state.priority()?.let { call to it } }
        .sortedBy { it.second }
        .map { it.first }
    return when {
        live.isNotEmpty() -> InCallState.Active(live)
        // A call disconnected, or the first update of a call that's already gone
        fallback is InCallState.Active -> InCallState.Ended
        else -> fallback
    }
}

private fun CallState.priority(): Int? = when (this) {
    CallState.RINGING -> 0
    CallState.ACTIVE -> 1
    CallState.HOLDING -> 3
    CallState.DISCONNECTING, CallState.DISCONNECTED -> null
    else -> 2 // dialing, connecting, new...
}
