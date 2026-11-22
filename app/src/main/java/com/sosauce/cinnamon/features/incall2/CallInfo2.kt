package com.sosauce.cinnamon.features.incall2

import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Immutable

@Immutable
data class CallInfo2(
    val id: String,
    // Caller identity
    val number: String?,
    val numberPresentation: Int,
    val contactName: String?,
    val contactPhotoUri: Uri?,
    // Call lifecycle
    val state: CallState2,
    val connectTimeMillis: Long?,
    val capabilities: CallCapabilities2,
    // Call metadata
    val direction: CallDirection2,
    val phoneAccountHandle: PhoneAccountHandle?,
    val parentCallId: String?,
    val childCallIds: List<String>
)

enum class CallState2 {
    NEW,
    CONNECTING,
    DIALING,
    RINGING,
    ACTIVE,
    HOLDING,
    DISCONNECTING,
    DISCONNECTED
}

enum class CallDirection2 {
    INCOMING,
    OUTGOING
}

@Immutable
data class CallCapabilities2(
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
    val isHd: Boolean = false
)