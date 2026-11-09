package com.sosauce.cinnamon.features.messaging.data.local.conversationSettings

import com.sosauce.cinnamon.features.messaging.domain.ConversationSettings

sealed class ConversationSettingActions {
    data class UpsertConversationSettings(val conversationSettings: ConversationSettings) : ConversationSettingActions()
    data class UpsertDraft(val draft: String) : ConversationSettingActions()
}