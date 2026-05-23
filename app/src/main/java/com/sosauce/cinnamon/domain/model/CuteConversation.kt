package com.sosauce.cinnamon.domain.model

/**
 * A conversation.
 * @param rawRecipients list of all people in this conversation (as phone numbers, excluding ourselves) only multiple if is a group chat
 * @param recipients Contact name or not of recipients
 */
data class CuteConversation(
    val threadId: Long = 0,
    val rawRecipients: List<String> = emptyList(),
    val recipients: List<String> = emptyList(),
    val snippet: String = "",
    val date: Long = 0,
    val read: Boolean = true,
    val isSenderBlocked: Boolean = false,
    val isGroupChat: Boolean = false,
    val draft: String = ""
)