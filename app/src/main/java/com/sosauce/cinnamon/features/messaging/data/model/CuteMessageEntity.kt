package com.sosauce.cinnamon.features.messaging.data.model

data class CuteMessageEntity(
    val id: Long,
    val body: String,
    val type: Int,
    val dateMillis: Long,
    val threadId: Long,
    val read: Boolean,
    val isMms: Boolean,
    val attachment: CuteAttachment? = null,
    val isScheduled: Boolean,
    val delivered: Boolean,
    /**
     * Address of whoever sent a received message, null for sent messages or when unknown
     */
    val senderAddress: String? = null
)