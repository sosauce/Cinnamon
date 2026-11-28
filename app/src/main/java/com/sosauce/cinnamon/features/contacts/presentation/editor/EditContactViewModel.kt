package com.sosauce.cinnamon.features.contacts.presentation.editor

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsActions
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsEntity
import com.sosauce.cinnamon.features.contacts.data.repository.ContactGroupsRepository
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.ContactAccount
import com.sosauce.cinnamon.features.contacts.domain.ContactGroup
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.cinnamon.features.contacts.domain.RawContactEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditContactViewModel(
    private val rawContactId: Long,
    private val prefilledNumber: String,
    private val contactSettingsDao: ContactSettingsDao,
    private val contactsRepository: ContactsRepository,
    private val contactGroupsRepository: ContactGroupsRepository
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
            viewModelScope.launch {
                val account = contactGroupsRepository.getRawContactAccount(rawContactId) ?: return@launch

                combine(
                    contactGroupsRepository.observeGroups(),
                    contactGroupsRepository.observeRawContactGroupIds(rawContactId)
                ) { groups, groupIds ->
                    groups.filter { it.account == account } to groupIds
                }.collectLatest { (groups, groupIds) ->
                    _state.update {
                        it.copy(groupAccount = account, groups = groups, groupIds = groupIds)
                    }
                }
            }

            viewModelScope.launch(Dispatchers.IO) {

                val rawContact = contactsRepository.fetchRawContactEdit(rawContactId)

                _state.update {
                    it.copy(
                        rawContact = rawContact,
                        isLoading = false
                    )
                }
                contactSettingsDao.getContactSettings(rawContact.contactId)
                    .collectLatest { settings ->
                        _state.update {
                            it.copy(
                                settings = settings
                                    ?: ContactSettingsEntity(contactId = rawContact.contactId)
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
            is EditContactAction.ToggleGroup -> viewModelScope.launch {
                val isMember = action.group.id in _state.value.groupIds
                if (!contactGroupsRepository.setMembership(rawContactId, action.group.id, !isMember)) {
                    _events.send(EditContactEvent.GroupError)
                }
            }

            is EditContactAction.CreateGroup -> viewModelScope.launch {
                val groupId = contactGroupsRepository.createGroup(action.title, action.account)
                if (groupId == null || !contactGroupsRepository.setMembership(rawContactId, groupId, true)) {
                    _events.send(EditContactEvent.GroupError)
                }
            }

            is EditContactAction.RenameGroup -> viewModelScope.launch {
                if (!contactGroupsRepository.renameGroup(action.group.id, action.title)) {
                    _events.send(EditContactEvent.GroupError)
                }
            }

            is EditContactAction.DeleteGroup -> viewModelScope.launch {
                if (!contactGroupsRepository.deleteGroup(action.group.id)) {
                    _events.send(EditContactEvent.GroupError)
                }
            }

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

@Stable
data class EditContactState(
    val isLoading: Boolean = false,
    val rawContact: RawContactEdit = RawContactEdit(),
    val settings: ContactSettingsEntity = ContactSettingsEntity(),
    val isCreateInsteadOfEdit: Boolean,
    val groupAccount: ContactAccount? = null,
    val groups: List<ContactGroup> = emptyList(),
    val groupIds: Set<Long> = emptySet()
)

sealed interface EditContactEvent {
    data object Success : EditContactEvent
    data object Error : EditContactEvent
    data object GroupError : EditContactEvent
}

sealed interface EditContactAction {
    data class SaveEditedContact(
        val editedRawContact: RawContactEdit
    ) : EditContactAction

    data class ToggleGroup(val group: ContactGroup) : EditContactAction
    data class CreateGroup(val title: String, val account: ContactAccount) : EditContactAction
    data class RenameGroup(val group: ContactGroup, val title: String) : EditContactAction
    data class DeleteGroup(val group: ContactGroup) : EditContactAction
}
