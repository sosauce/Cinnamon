package com.sosauce.cinnamon.data.conversation_settings

import com.sosauce.cinnamon.domain.model.ConversationSettings

sealed class ConversationSettingActions {
    data class UpsertConversationSettings(val conversationSettings: ConversationSettings) :
        ConversationSettingActions()
}