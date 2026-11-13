package com.sosauce.cinnamon.features.messaging.data.model

import androidx.compose.ui.util.fastAny
import com.sosauce.cinnamon.features.messaging.domain.Participant

data class CuteConversationEntity(
    val threadId: Long = 0,
    val participants: List<Participant> = emptyList(),
    val snippet: String = "",
    val date: Long = 0,
    val unreadCount: Int = 0,
    val read: Boolean
)


fun CuteConversationEntity.matches(query: String): Boolean {
    return query.isBlank() ||
            snippet.contains(query, true) ||
            participants.fastAny { it.displayName.contains(query, true) }
}
