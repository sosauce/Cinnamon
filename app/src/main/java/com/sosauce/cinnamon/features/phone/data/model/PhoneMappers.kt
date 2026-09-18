package com.sosauce.cinnamon.features.phone.data.model

import android.content.Context
import android.provider.CallLog
import android.text.format.DateUtils
import androidx.core.net.toUri
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.core.utils.toTime
import com.sosauce.cinnamon.features.phone.domain.CallPresentation
import com.sosauce.cinnamon.features.phone.domain.CallType
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
import com.sosauce.cinnamon.features.phone.domain.CuteVoicemail

fun CuteCallLogEntity.toDomain(context: Context): CuteCallLog2 {


    val callType = when(type) {
        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
        else -> CallType.REJECTED
    }

    val presentation = when(presentation) {
        CallLog.Calls.PRESENTATION_ALLOWED -> CallPresentation.ALLOWED
        CallLog.Calls.PRESENTATION_PAYPHONE -> CallPresentation.PAYPHONE
        CallLog.Calls.PRESENTATION_RESTRICTED -> CallPresentation.RESTRICTED
        CallLog.Calls.PRESENTATION_UNAVAILABLE -> CallPresentation.UNAVAILABLE
        CallLog.Calls.PRESENTATION_UNKNOWN -> CallPresentation.UNKNOWN
        else -> CallPresentation.UNKNOWN
    }

    val formattedDate = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_ABBREV_MONTH)
    val duration = if (duration <= 0) null else DateUtils.formatElapsedTime(duration)

    return CuteCallLog2(
        id = id,
        number = number,
        displayName = cachedName?.ifEmpty { null } ?: number.beautifyNumber(),
        callType = callType,
        date = formattedDate,
        time = date.toTime(),
        duration = duration,
        location = location,
        presentation = presentation,
        photo = photo?.toUri()
    )
}

fun CuteVoicemailEntity.toDomain(context: Context): CuteVoicemail {

    val formattedDate = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_ABBREV_MONTH)
    val formattedDuration = DateUtils.formatElapsedTime(duration)

    return CuteVoicemail(
        id = id,
        number = number,
        displayName = name ?: number.beautifyNumber(),
        date = formattedDate,
        time = date.toTime(),
        duration = formattedDuration,
        photo = photo?.toUri(),
        voicemail = voicemail.toUri()
    )
}