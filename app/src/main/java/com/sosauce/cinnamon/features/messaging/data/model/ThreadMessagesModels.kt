package com.sosauce.cinnamon.features.messaging.data.model

import com.sosauce.cinnamon.features.messaging.domain.messageListKey

/**
 * A slice of a thread's messages plus the total amount of messages in that thread.
 */
data class MessagesPage<T>(
    val messages: List<T>,
    val totalCount: Int
)

/**
 * Minimal representation of a message used to search through a thread.
 */
data class SearchableMessage(
    val id: Long,
    val isMms: Boolean,
    val text: String
) {
    val key: String
        get() = messageListKey(id = id, isMms = isMms, isScheduled = false)
}
