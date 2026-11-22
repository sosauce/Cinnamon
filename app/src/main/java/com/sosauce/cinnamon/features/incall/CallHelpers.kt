package com.sosauce.cinnamon.features.incall

import android.os.Build
import android.telecom.Call

fun Call.Details.number(): String? {
    return handle?.schemeSpecificPart ?: gatewayInfo?.originalAddress?.schemeSpecificPart
}

fun Call.Details.contactNameApi30(): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        contactDisplayName?.takeIf { it.isNotBlank() }
    } else null
}

val Call.Details.contactPhotoApi34
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) contactPhotoUri else null

val Call.compatState
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        details?.state ?: Call.STATE_NEW
    } else {
        state
    }

fun Call.toCallDirection(): CallDirection {
    val direction =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) details?.callDirection else null
    return when (direction) {
        Call.Details.DIRECTION_INCOMING -> CallDirection.INCOMING
        Call.Details.DIRECTION_OUTGOING -> CallDirection.OUTGOING
        else -> if (compatState == Call.STATE_RINGING) {
            CallDirection.INCOMING
        } else {
            CallDirection.OUTGOING
        }
    }
}

val Call.Details.isHd: Boolean
    get() = hasProperty(Call.Details.PROPERTY_HIGH_DEF_AUDIO)

fun Int.toCallState(): CallState {
    return when (this) {
        Call.STATE_NEW -> CallState.NEW
        Call.STATE_CONNECTING -> CallState.CONNECTING
        Call.STATE_DIALING -> CallState.DIALING
        Call.STATE_RINGING -> CallState.RINGING
        Call.STATE_ACTIVE -> CallState.ACTIVE
        Call.STATE_HOLDING -> CallState.HOLDING
        Call.STATE_DISCONNECTING -> CallState.DISCONNECTING
        Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
        Call.STATE_PULLING_CALL -> CallState.CONNECTING
        else -> CallState.NEW
    }
}

fun Call.toCapabilities(): CallCapabilities {
    val callState = compatState
    val canHold = details.hasCapability(Call.Details.CAPABILITY_HOLD)

    return CallCapabilities(
        canHold = canHold && callState != Call.STATE_HOLDING,
        canUnhold = canHold && callState == Call.STATE_HOLDING,
        canAnswer = callState == Call.STATE_RINGING || callState == Call.STATE_SIMULATED_RINGING,
        canReject = callState == Call.STATE_RINGING || callState == Call.STATE_SIMULATED_RINGING,
        canDisconnect = when (callState) {
            Call.STATE_RINGING,
            Call.STATE_SIMULATED_RINGING,
            Call.STATE_DIALING,
            Call.STATE_CONNECTING,
            Call.STATE_ACTIVE,
            Call.STATE_HOLDING,
            Call.STATE_DISCONNECTING,
            Call.STATE_PULLING_CALL -> true

            else -> false
        },
        canMerge = details.hasCapability(Call.Details.CAPABILITY_MERGE_CONFERENCE),
        canSwap = details.hasCapability(Call.Details.CAPABILITY_SWAP_CONFERENCE),
        canRespondViaText = details.hasCapability(Call.Details.CAPABILITY_RESPOND_VIA_TEXT),
        canMute = details.hasCapability(Call.Details.CAPABILITY_MUTE),
        isHd = details?.isHd == true,
        isWifi = details?.hasProperty(Call.Details.PROPERTY_WIFI) == true,
        supportsDtmf = when (callState) {
            Call.STATE_DIALING,
            Call.STATE_CONNECTING,
            Call.STATE_ACTIVE,
            Call.STATE_HOLDING -> true

            else -> false
        }
    )
}

private fun Call.Details?.hasCapability(capability: Int): Boolean {
    return this != null && callCapabilities and capability != 0
}