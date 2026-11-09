@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences,
    private val contactSettingsDao: ContactSettingsDao
) : ViewModel() {

    val textFieldState = TextFieldState()

    private val searchQuery = snapshotFlow { textFieldState.text.toString() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()

    private val _state = MutableStateFlow(
        ContactsState(
            isLoading = true
        )
    )

    private val accountFilter = _state.map { it.accountFilter }.distinctUntilChanged()

    val contacts: Flow<PagingData<ContactsListItem>> = combine(
        searchQuery,
        userPreferences.getSortContactsAscending(),
        accountFilter
    ) { query, asc, account -> Triple(query, asc, account) }
        .distinctUntilChanged()
        .flatMapLatest { (query, asc, account) ->
            contactsRepository.pagedContacts(
                ascending = asc,
                isFiltered = query.isNotEmpty() || !account.isAllAccounts(),
                filter = contactFilter(query, account)
            )
        }
        .map { pagingData ->
            pagingData
                .map<CuteContact, ContactsListItem> { ContactsListItem.Contact(it) }
                .insertSeparators { before, after ->
                    val next = (after as? ContactsListItem.Contact)?.contact ?: return@insertSeparators null
                    val previous = (before as? ContactsListItem.Contact)?.contact
                    if (previous?.initial != next.initial) ContactsListItem.Header(next.initial, next.id) else null
                }
        }
        .cachedIn(viewModelScope)

    val state = combine(
        _state,
        contactsRepository.fetchLatestFavoriteContacts(),
        contactsRepository.fetchLatestAccountsToCount(),
        searchQuery
    ) { state, favorites, accountsToCount, query ->
        state.copy(
            isLoading = false,
            favorites = favorites.fastFilter(contactFilter(query, state.accountFilter)),
            accountsToCount = accountsToCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ContactsState(isLoading = true)
    )

    private fun contactFilter(query: String, account: String?): (CuteContact) -> Boolean = { contact ->
        (query.isEmpty() || contact.searchIndex.contains(query, true)) &&
                (account.isAllAccounts() || contact.accountName.ifEmpty { "Device" } == account)
    }

    private fun String?.isAllAccounts() = this == null || this == "All"


    fun handleContactsAction(action: ContactsAction) {
        when (action) {
            is ContactsAction.ChangeAccountFiltering -> {
                _state.update {
                    it.copy(
                        accountFilter = action.accountName
                    )
                }
            }

            is ContactsAction.DeleteContacts -> {
                viewModelScope.launch(Dispatchers.IO) {
                    contactsRepository.deleteContacts(action.ids)
                    contactSettingsDao.deleteContactsSettings(action.ids)
                }
            }

            is ContactsAction.ToggleFavorite -> {
                viewModelScope.launch(Dispatchers.IO) {
                    contactsRepository.toggleFavorite(action.contacts)
                }
            }
        }
    }

}

data class ContactsState(
    val isLoading: Boolean = false,
    val favorites: List<CuteContact> = emptyList(),
    val accountsToCount: Map<String, Int> = emptyMap(),
    val accountFilter: String? = null
)

sealed interface ContactsAction {
    data class ChangeAccountFiltering(val accountName: String) : ContactsAction
    data class DeleteContacts(
        val ids: List<Long>
    ) : ContactsAction

    data class ToggleFavorite(val contacts: List<CuteContact>) : ContactsAction
}

sealed interface ContactsListItem {
    data class Header(val letter: Char, val beforeContactId: Long) : ContactsListItem
    data class Contact(val contact: CuteContact) : ContactsListItem
}

private val CuteContact.initial
    get() = displayName.firstOrNull()?.uppercaseChar() ?: '#'
