package com.sosauce.cinnamon.features.contacts.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsEntity
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsActions
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditContactViewModel(
    private val contact: CuteContact,
    private val details: CuteContactDetails,
    private val contactSettingsDao: ContactSettingsDao,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val isCreateInsteadOfEdit = contact.id == 0L
    private val _state =
        MutableStateFlow(EditContactState(contact, details, isCreateInsteadOfEdit = isCreateInsteadOfEdit))
    val state = _state.asStateFlow()


    init {
        // Can't fetch settings for a contact that doesn't exist yet
        if (!isCreateInsteadOfEdit) {
            viewModelScope.launch(Dispatchers.IO) {
                contactSettingsDao.getContactSettings(contact.id).collectLatest { settings ->
                    _state.update {
                        it.copy(
                            settings = settings ?: ContactSettingsEntity(contactId = contact.id)
                        )
                    }
                }
            }
        }
    }


    fun handleContactSettingsAction(action: ContactSettingsActions) {
        when (action) {
            is ContactSettingsActions.UpsertContactSettings -> {
                viewModelScope.launch(Dispatchers.IO) {
                    contactSettingsDao.upsertContact(action.contactSettingsEntity)
                }
            }
        }
    }

    fun handleEditContactAction(action: EditContactAction) {
        when (action) {
            is EditContactAction.SaveEditedContact -> {
                viewModelScope.launch {
                    contactsRepository.createOrEditContact(action.editedContact, action.editedDetails)
                }
            }
        }
    }

}


data class EditContactState(
    val contact: CuteContact = CuteContact(),
    val details: CuteContactDetails = CuteContactDetails(),
    val settings: ContactSettingsEntity = ContactSettingsEntity(),
    val isCreateInsteadOfEdit: Boolean
)

sealed interface EditContactAction {
    data class SaveEditedContact(
        val editedContact: CuteContact,
        val editedDetails: CuteContactDetails
    ) : EditContactAction
}
