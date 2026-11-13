@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences,
    private val contactSettingsDao: ContactSettingsDao
) : ViewModel() {

    val textFieldState = TextFieldState()

    private val searchQuery = snapshotFlow { textFieldState.text.toString().trim() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()

    private val accountFilter = MutableStateFlow<String?>(null)

    private val sortAscending = userPreferences.getSortContactsAscending().distinctUntilChanged()

    /**
     * contactId -> account name, null until loaded
     */
    private val contactAccounts = contactsRepository
        .observeContactChanges()
        .mapLatest { contactsRepository.getContactAccounts() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Ids of the contacts matching the search query, null when not searching
     */
    private val searchResults: Flow<Set<Long>?> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(null)
            } else {
                // refresh results when contacts change
                contactsRepository
                    .observeContactChanges()
                    .mapLatest { contactsRepository.searchContactIds(query) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Search + account filtering, applied to both favorites and the paged contacts
     */
    private val selection: Flow<String?> = combine(
        searchResults,
        accountFilter,
        contactAccounts
    ) { results, filter, accounts ->
        val accountIds = if (filter == null || filter == ALL_ACCOUNTS) {
            null
        } else {
            accounts?.filterValues { it == filter }?.keys
        }

        listOfNotNull(results, accountIds)
            .joinToString(" AND ") { it.toContactIdSelection() }
            .ifEmpty { null }
    }.distinctUntilChanged()

    private val pager = ContactsPager(
        contactsRepository = contactsRepository,
        scope = viewModelScope,
        selection = selection,
        sortAscending = sortAscending
    )

    /**
     * Non favorite contacts, paged, with a letter header before each group.
     */
    val contacts: Flow<PagingData<ContactsListItem>> = pager.contacts

    val state = combine(
        pager.favorites,
        contactAccounts,
        accountFilter,
        pager.loaded
    ) { favorites, accounts, filter, loaded ->
        ContactsState(
            isLoading = !loaded || favorites == null,
            favorites = favorites.orEmpty(),
            accountsToCount = accounts?.let {
                mapOf(ALL_ACCOUNTS to it.size) + it.values.groupingBy { name -> name }.eachCount()
            } ?: emptyMap(),
            accountFilter = filter
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ContactsState(isLoading = true)
    )

    fun handleContactsAction(action: ContactsAction) {
        when (action) {
            is ContactsAction.ChangeAccountFiltering -> accountFilter.value = action.accountName
            is ContactsAction.DeleteContacts -> viewModelScope.launch(Dispatchers.IO) {
                contactsRepository.deleteContacts(action.ids)
                contactSettingsDao.deleteContactsSettings(action.ids)
            }
            is ContactsAction.ToggleFavorite -> viewModelScope.launch(Dispatchers.IO) {
                contactsRepository.toggleFavorite(action.contacts)
            }
        }
    }

    companion object {
        private const val ALL_ACCOUNTS = "All"
    }
}

data class ContactsState(
    val isLoading: Boolean = false,
    /**
     * Favorite contacts aren't paged, the others are in [ContactsViewModel.contacts]
     */
    val favorites: List<CuteContact> = emptyList(),
    val accountsToCount: Map<String, Int> = emptyMap(),
    val accountFilter: String? = null
)

sealed interface ContactsAction {
    data class ChangeAccountFiltering(val accountName: String) : ContactsAction
    data class DeleteContacts(val ids: List<Long>) : ContactsAction
    data class ToggleFavorite(val contacts: List<CuteContact>) : ContactsAction
}
