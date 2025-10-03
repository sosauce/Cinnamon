package com.sosauce.cuteconnect.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cuteconnect.data.contact_settings.ContactSettings
import com.sosauce.cuteconnect.data.contact_settings.ContactSettingsActions
import com.sosauce.cuteconnect.data.contact_settings.ContactSettingsDao
import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingActions
import com.sosauce.cuteconnect.domain.model.ConversationSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel(
    private val contactId: Long = 0,
    private val contactSettingsDao: ContactSettingsDao
) : ViewModel() {


    val contactSettings = contactSettingsDao.getConversationSettings(contactId)
        .map { it ?: ContactSettings(contactId.toInt()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ContactSettings(contactId.toInt())
        )

    fun handleContactSettingsActions(action: ContactSettingsActions) {
        when(action) {
            is ContactSettingsActions.UpsertContactSettings -> {
                viewModelScope.launch(Dispatchers.IO) {
                    contactSettingsDao.upsertContact(action.contactSettings)
                }
            }
        }
    }

}