package com.sosauce.cinnamon.features.messaging.data.model

import android.content.Context
import android.provider.Telephony
import android.text.format.DateUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.sosauce.cinnamon.core.utils.toDate
import com.sosauce.cinnamon.core.utils.toTime
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsEntity
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessageEntity
import com.sosauce.cinnamon.features.messaging.domain.ConversationSettings
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.domain.MessageType

fun CuteConversationEntity.toCuteConversation(draft: String): CuteConversation {
    return CuteConversation(
        threadId = threadId,
        participants = participants,
        snippet = snippet,
        date = date.toDate(),
        read = read,
        draft = draft
    )
}

fun CuteMessageEntity.toCuteMessage(context: Context): CuteMessage {

    val type = when(type) {
        Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageType.RECEIVED
        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> MessageType.SENDING
        Telephony.Sms.MESSAGE_TYPE_SENT -> MessageType.SENT
        Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageType.FAILED
        else -> MessageType.SENT
    }

    val dateTime = DateUtils.formatDateTime(context, dateMillis, DateUtils.FORMAT_ABBREV_MONTH)
    return CuteMessage(
        id = id,
        threadId = threadId,
        body = body,
        date = dateTime,
        time = dateMillis.toTime(),
        read = read,
        isMms = isMms,
        isScheduled = isScheduled,
        delivered = delivered,
        type = type,
        timestamp = dateMillis,
        attachment = attachment
    )
}

fun ScheduledMessageEntity.toCuteMessage(context: Context): CuteMessage {

    val dateTime = DateUtils.formatDateTime(context, sendAt, DateUtils.FORMAT_ABBREV_MONTH)

    return CuteMessage(
        id = id,
        threadId = threadId,
        body = message,
        date = dateTime,
        time = sendAt.toTime(),
        read = true,
        isMms = false,
        isScheduled = true,
        delivered = false,
        type = MessageType.SENT,
        timestamp = sendAt
    )
}

fun ConversationSettingsEntity.toConversationSettings(): ConversationSettings {

    val wallpaperUri = if (wallpaper.isEmpty()) null else wallpaper.toUri()
    val color = if (color == -1) null else Color(color)

    return ConversationSettings(
        id = id,
        threadId = threadId,
        draft = draft,
        wallpaper = wallpaperUri,
        wallpaperBlurIntensity = wallpaperBlurIntensity,
        color = color
    )
}

fun ConversationSettings.toEntity(): ConversationSettingsEntity {
    return ConversationSettingsEntity(
        id = id,
        threadId = threadId,
        draft = draft,
        wallpaper = wallpaper.toString(),
        wallpaperBlurIntensity = wallpaperBlurIntensity,
        color = color?.toArgb() ?: -1
    )
}