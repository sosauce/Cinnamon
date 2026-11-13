package com.sosauce.cinnamon.features.messaging.domain

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri

data class CuteConversation(
    val threadId: Long = 0,
    val participants: List<Participant> = emptyList(),
    val snippet: String = "",
    val date: String = "",
    val read: Boolean = true,
    val unreadCount: Int = 0,
    val draft: String = "",
    /**
     * Amount of messages matching the current search query, 0 when not searching through messages
     */
    val matchingMessagesCount: Int = 0
) {
    val isGroupChat = participants.size > 1

    val name = buildString {
        participants.fastForEachIndexed { index, participant ->
            append(participant.displayName)
            if (index != participants.lastIndex) {
                append(", ")
            }
        }
    }

    val isAnyBlocked = participants.fastAny { it.isBlocked }

    /**
     * For non-group chats
     */
    val photo = participants.firstOrNull()?.photoUri

    /**
     * Full quality version of [photo], only use where a large avatar is shown
     */
    val hqPhoto = participants.firstOrNull()?.hqPhotoUri

    val isSoloParticipantBlocked = participants.firstOrNull()?.isBlocked == true
}

data class Participant(
    val rawNumber: String,
    val displayName: String,
    val photoUriString: String?,
    val isBlocked: Boolean,
    val contactId: Long? = null,
    /**
     * Full quality photo, prefer [photoUriString] unless a large avatar is shown
     */
    val hqPhotoUriString: String? = null
) {
    val photoUri
        get() = photoUriString?.toUri()

    val hqPhotoUri
        get() = (hqPhotoUriString ?: photoUriString)?.toUri()
}

fun List<Participant>.isGroupChat(): Boolean = size > 1

fun List<Participant>.name(): String {

    if (isEmpty()) return ""

    return if (size > 1) {
        this.joinToString(", ") { it.displayName }
    } else first().displayName
}