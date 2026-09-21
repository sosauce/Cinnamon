package com.sosauce.cinnamon.features.contacts.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsActions
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsEntity
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import com.sosauce.cinnamon.features.contacts.domain.RawContactEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditContactViewModel(
    private val rawContactId: Long,
    private val prefilledNumber: String,
    private val contactSettingsDao: ContactSettingsDao,
    private val contactsRepository: ContactsRepository
) : ViewModel() {


    val isCreateInsteadOfEdit = rawContactId == Long.MAX_VALUE
    private val _events = Channel<EditContactEvent>()
    val events = _events.receiveAsFlow()
    private val _state =
        MutableStateFlow(
            EditContactState(
                isCreateInsteadOfEdit = isCreateInsteadOfEdit,
                isLoading = true
            )
        )
    val state = _state.asStateFlow()


    init {
        if (!isCreateInsteadOfEdit) {
            viewModelScope.launch(Dispatchers.IO) {

                val rawContact = contactsRepository.fetchRawContactEdit(rawContactId)

                _state.update {
                    it.copy(
                        rawContact = rawContact,
                        isLoading = false
                    )
                }
                contactSettingsDao.getContactSettings(rawContact.contactId).collectLatest { settings ->
                    _state.update {
                        it.copy(
                            settings = settings ?: ContactSettingsEntity(contactId = rawContact.contactId)
                        )
                    }
                }
            }
        } else {
            // creating

            val numbers = if (prefilledNumber.isNotEmpty()) {
                listOf(
                    ContactPhone(
                        number = prefilledNumber,
                        type = 0,
                        isDefault = true
                    )
                )
            } else emptyList()

            _state.update {
                it.copy(
                    rawContact = RawContactEdit(
                        rawContactId = null,
                        phoneNumbers = numbers
                    ),
                    isLoading = false
                )
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
                    val edited = action.editedRawContact
                    val success = contactsRepository.createOrEditContact(edited)

                    if (success) {
                        _events.send(EditContactEvent.Success)
                    } else {
                        _events.send(EditContactEvent.Error)
                    }
                }
            }
        }
    }

}


data class EditContactState(
    val isLoading: Boolean = false,
    val rawContact: RawContactEdit = RawContactEdit(),
    val settings: ContactSettingsEntity = ContactSettingsEntity(),
    val isCreateInsteadOfEdit: Boolean
)
sealed interface EditContactEvent {
    data object Success: EditContactEvent
    data object Error: EditContactEvent
}
sealed interface EditContactAction {
    data class SaveEditedContact(
        val editedRawContact: RawContactEdit
    ) : EditContactAction
}
