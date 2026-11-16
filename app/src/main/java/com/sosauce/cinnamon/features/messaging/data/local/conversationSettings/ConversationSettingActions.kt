package com.sosauce.cinnamon.features.messaging.data.local.conversationSettings

import com.sosauce.cinnamon.features.messaging.data.model.toEntity
import com.sosauce.cinnamon.features.messaging.domain.ConversationSettings

sealed class ConversationSettingActions {
    data class UpsertConversationSettings(val conversationSettings: ConversationSettings) :
        ConversationSettingActions()

    data class UpsertDraft(val draft: String) : ConversationSettingActions()
}

/**
 * Runs this action for the conversation [threadId], shared by every screen editing its settings
 */
suspend fun ConversationSettingActions.perform(
    conversationSettingsDao: ConversationSettingsDao,
    threadId: Long
) {
    when (this) {
        is ConversationSettingActions.UpsertConversationSettings -> {
            conversationSettingsDao.upsertConversation(conversationSettings.toEntity())
        }

        is ConversationSettingActions.UpsertDraft -> {
            conversationSettingsDao.upsertDraft(threadId, draft)
        }
    }
}
