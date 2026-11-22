package com.sosauce.cinnamon.features.incall

import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Immutable

@Immutable
data class CallInfo(
    val id: String,
    // Caller identity
    val number: String?,
    val numberPresentation: Int,
    val contactName: String?,
    val contactPhotoUri: Uri?,
    // Call lifecycle
    val state: CallState,
    val connectTimeMillis: Long?,
    val capabilities: CallCapabilities,
    // Call metadata
    val direction: CallDirection,
    val phoneAccountHandle: PhoneAccountHandle?,
    val parentCallId: String?,
    val childCallIds: List<String>
)

enum class CallState {
    NEW,
    CONNECTING,
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTING,
    DISCONNECTED
}

enum class CallDirection {
    INCOMING,
    OUTGOING
}

@Immutable
data class CallCapabilities(
    val canHold: Boolean = false,
    val canUnhold: Boolean = false,
    val canAnswer: Boolean = false,
    val canReject: Boolean = false,
    val canDisconnect: Boolean = false,
    val canMerge: Boolean = false,
    val canSwap: Boolean = false,
    val canRespondViaText: Boolean = false,
    val canMute: Boolean = false,
    val supportsDtmf: Boolean = false,
    val isHd: Boolean = false,
    val isWifi: Boolean = false
)