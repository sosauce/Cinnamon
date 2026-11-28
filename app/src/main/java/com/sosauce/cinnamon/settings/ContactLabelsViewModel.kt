package com.sosauce.cinnamon.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.contacts.data.repository.ContactGroupsRepository
import com.sosauce.cinnamon.features.contacts.domain.ContactAccount
import com.sosauce.cinnamon.features.contacts.domain.ContactGroup
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactLabelsViewModel(
    private val contactGroupsRepository: ContactGroupsRepository
) : ViewModel() {

    private val _events = Channel<ContactLabelsEvent>()
    val events = _events.receiveAsFlow()

    val state = combine(
        contactGroupsRepository.observeGroups(),
        contactGroupsRepository.observeAccounts()
    ) { groups, accounts ->
        ContactLabelsState(groups = groups, accounts = accounts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactLabelsState())

    fun handleContactLabelsAction(action: ContactLabelsAction) {
        viewModelScope.launch {
            val success = when (action) {
                is ContactLabelsAction.Create ->
                    contactGroupsRepository.createGroup(action.title, action.account) != null

                is ContactLabelsAction.Rename ->
                    contactGroupsRepository.renameGroup(action.group.id, action.title)

                is ContactLabelsAction.Delete ->
                    contactGroupsRepository.deleteGroup(action.group.id)
            }
            if (!success) _events.send(ContactLabelsEvent.Error)
        }
    }
}

@Stable
data class ContactLabelsState(
    val groups: List<ContactGroup> = emptyList(),
    val accounts: List<ContactAccount> = emptyList()
)

sealed interface ContactLabelsEvent {
    data object Error : ContactLabelsEvent
}

sealed interface ContactLabelsAction {
    data class Create(val title: String, val account: ContactAccount) : ContactLabelsAction
    data class Rename(val group: ContactGroup, val title: String) : ContactLabelsAction
    data class Delete(val group: ContactGroup) : ContactLabelsAction
}
