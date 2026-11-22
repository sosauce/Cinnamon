package com.sosauce.cinnamon.features.incall2

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

val Call.Details.compatConnectTimeMillis: Long
    get() = connectTimeMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

fun Call.toCallDirection(): CallDirection2 {
    val direction =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) details?.callDirection else null
    return when (direction) {
        Call.Details.DIRECTION_INCOMING -> CallDirection2.INCOMING
        Call.Details.DIRECTION_OUTGOING -> CallDirection2.OUTGOING
        else -> if (compatState == Call.STATE_RINGING) {
            CallDirection2.INCOMING
        } else {
            CallDirection2.OUTGOING
        }
    }
}

val Call.Details.isHd: Boolean
    get() = hasProperty(Call.Details.PROPERTY_HIGH_DEF_AUDIO)

fun Int.toCallState(): CallState2 {
    return when (this) {
        Call.STATE_NEW -> CallState2.NEW
        Call.STATE_CONNECTING -> CallState2.CONNECTING
        Call.STATE_DIALING -> CallState2.DIALING
        Call.STATE_RINGING -> CallState2.RINGING
        Call.STATE_ACTIVE -> CallState2.ACTIVE
        Call.STATE_HOLDING -> CallState2.HOLDING
        Call.STATE_DISCONNECTING -> CallState2.DISCONNECTING
        Call.STATE_DISCONNECTED -> CallState2.DISCONNECTED
        Call.STATE_PULLING_CALL -> CallState2.CONNECTING
        else -> CallState2.NEW
    }
}

fun Call.toCapabilities(): CallCapabilities2 {
    val callState = compatState
    val canHold = details.hasCapability(Call.Details.CAPABILITY_HOLD)

    return CallCapabilities2(
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