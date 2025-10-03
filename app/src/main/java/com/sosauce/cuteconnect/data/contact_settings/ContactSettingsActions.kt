package com.sosauce.cuteconnect.data.contact_settings

import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingActions
import com.sosauce.cuteconnect.domain.model.ConversationSettings

sealed class ContactSettingsActions {
    data class UpsertContactSettings(val contactSettings: ContactSettings) : ContactSettingsActions()
}