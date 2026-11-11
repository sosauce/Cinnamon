package com.sosauce.cinnamon.features.phone.domain

import android.net.Uri
import androidx.compose.runtime.Stable

/**
 * @param sim Will be null if user only has one active sim
 */
@Stable
data class CuteCallLog(
    val id: Long,
    val number: String,
    val displayName: String,
    val callType: CallType,
    val date: String,
    val time: String,
    val duration: String?,
    val location: String?,
    val presentation: CallPresentation,
    val photo: Uri?,
    val sim: CuteSimCard?
) {
    // in most case this should do it
    val hasContactAssociated: Boolean
        get() = displayName.any { it.isLetter() }
}


enum class CallPresentation {
    ALLOWED,
    UNAVAILABLE,
    UNKNOWN,
    RESTRICTED,
    PAYPHONE
}

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED
}