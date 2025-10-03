package com.sosauce.cuteconnect.domain.model

/**
 * A conversation.
 * @param recipients list of all people in this conversation (as phone numbers, excluding ourselves) only multiple if is a group chat
 * @param contacts contacts associated to this thread
 */
data class CuteConversation(
    val threadId: Long,
    val contacts: List<CuteContact>,
    //val contactsId: List<Long>,
    val recipients: List<String>,
    val snippet: String,
    val date: Long,
    val read: Boolean,
    val isSenderBlocked: Boolean,
    val isGroupChat: Boolean
)