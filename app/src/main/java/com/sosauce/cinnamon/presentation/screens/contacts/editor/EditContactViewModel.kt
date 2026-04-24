package com.sosauce.cinnamon.presentation.screens.contacts.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.contact_settings.ContactSettings
import com.sosauce.cinnamon.data.contact_settings.ContactSettingsActions
import com.sosauce.cinnamon.data.contact_settings.ContactSettingsDao
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditContactViewModel(
    private val contact: CuteContact,
    private val contactSettingsDao: ContactSettingsDao,
    private val contactsRepository: ContactsRepository
): ViewModel() {

    private val isCreateInsteadOfEdit = contact.id == 0L
    private val _state = MutableStateFlow(EditContactState(contact, isCreateInsteadOfEdit = isCreateInsteadOfEdit))

    val state = _state.asStateFlow()




    // TODO CANT FETCH OR UPSERT SETTINGS FOR A CONTACTS THAT'S GETTING CREATED
    init {
        viewModelScope.launch(Dispatchers.IO) {
            contactSettingsDao.getConversationSettings(contact.id).collectLatest { settings ->
                _state.update {
                    it.copy(
                        settings = settings ?: ContactSettings(contact.id.toInt())
                    )
                }
            }
        }
    }


    fun handleContactSettingsAction(action: ContactSettingsActions) {
        when(action) {
            is ContactSettingsActions.UpsertContactSettings -> {
                viewModelScope.launch(Dispatchers.IO) {
                    contactSettingsDao.upsertContact(action.contactSettings)
                }
            }
        }
    }

    fun handleEditContactAction(action: EditContactAction) {
        when(action) {
            is EditContactAction.SaveEditedContact -> {
                viewModelScope.launch {
                    contactsRepository.createOrEditContact(action.editedContact, true)
                }
            }
        }
    }

}


data class EditContactState(
    val contact: CuteContact = CuteContact(),
    val settings: ContactSettings = ContactSettings(),
    val isCreateInsteadOfEdit: Boolean
)

sealed interface EditContactAction {
    data class SaveEditedContact(val editedContact: CuteContact) : EditContactAction
}