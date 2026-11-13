package com.sosauce.cinnamon.features.messaging.domain

import com.sosauce.cinnamon.features.messaging.data.model.CuteAttachment

data class CuteMessage(
    val id: Long,
    val body: String,
    val type: MessageType,
    val date: String,
    val time: String,
    val timestamp: Long,
    val threadId: Long,
    val read: Boolean,
    val isMms: Boolean,
    val attachment: CuteAttachment? = null,
    val isScheduled: Boolean,
    val delivered: Boolean
)
/**
 * Unique key of a message across SMS, MMS and scheduled messages (ids can collide between them).
 */
fun messageListKey(id: Long, isMms: Boolean, isScheduled: Boolean): String {
    val type = when {
        isScheduled -> "scheduled"
        isMms -> "mms"
        else -> "sms"
    }
    return "${type}_$id"
}

val CuteMessage.listKey: String
    get() = messageListKey(id = id, isMms = isMms, isScheduled = isScheduled)

enum class MessageType {
    SENT,
    RECEIVED,
    FAILED,
    SENDING
}