@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.repository.ContactGroupsRepository
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.ContactsDeletionException
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences,
    private val contactSettingsDao: ContactSettingsDao,
    private val contactGroupsRepository: ContactGroupsRepository
) : ViewModel() {

    private val _events = Channel<ContactsEvent>()
    val events = _events.receiveAsFlow()

    val textFieldState = TextFieldState()

    private val searchQuery = snapshotFlow { textFieldState.text.toString().trim() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()

    private val accountFilter = MutableStateFlow<String?>(null)

    private val labelFilter = MutableStateFlow<String?>(null)

    private val sortAscending = userPreferences.getSortContactsAscending().distinctUntilChanged()

    // Assumed until the screen says otherwise. Observing/querying contacts only works as the
    // default SMS or dialer app, so everything depending on it restarts when this changes
    private val isDefaultApp = MutableStateFlow(true)

    /**
     * contactId -> account name, null until loaded
     */
    private val contactAccounts = isDefaultApp
        .flatMapLatest { contactsRepository.observeContactChanges() }
        .mapLatest { contactsRepository.getContactAccounts() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val groups = isDefaultApp
        .flatMapLatest { contactGroupsRepository.observeGroups() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val labelsToContactIds = combine(
        groups,
        isDefaultApp.flatMapLatest { contactGroupsRepository.observeMemberships() }
    ) { groups, memberships ->
        groups.groupBy { it.title }.mapValues { (_, sameTitle) ->
            sameTitle.flatMapTo(mutableSetOf()) { memberships[it.id].orEmpty() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Ids of the contacts matching the search query, null when not searching
     */
    private val searchResults: Flow<Set<Long>?> = isDefaultApp
        .flatMapLatest { contactsRepository.searchResults(searchQuery) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Search + account filtering, applied to both favorites and the paged contacts
     */
    private val selection: Flow<String?> = combine(
        searchResults,
        accountFilter,
        contactAccounts,
        labelFilter,
        labelsToContactIds
    ) { results, filter, accounts, label, labels ->
        val accountIds = if (filter == null || filter == ALL_ACCOUNTS) {
            null
        } else {
            accounts?.filterValues { it == filter }?.keys
        }

        listOfNotNull(results, accountIds, label?.let { labels[it] })
            .joinToString(" AND ") { it.toContactIdSelection() }
            .ifEmpty { null }
    }.distinctUntilChanged()

    private val pager = ContactsPager(
        contactsRepository = contactsRepository,
        scope = viewModelScope,
        selection = selection,
        sortAscending = sortAscending,
        refresh = isDefaultApp
    )

    /**
     * Non favorite contacts, paged, with a letter header before each group.
     */
    val contacts: Flow<PagingData<ContactsListItem>> = pager.contacts

    val state = combine(
        combine(
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
        },
        labelsToContactIds,
        labelFilter
    ) { state, labels, label ->
        state.copy(
            labelsToCount = labels.mapValues { it.value.size },
            labelFilter = label?.takeIf { it in labels }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ContactsState(isLoading = true)
    )

    fun handleContactsAction(action: ContactsAction) {
        when (action) {
            is ContactsAction.ChangeAccountFiltering -> accountFilter.value = action.accountName
            is ContactsAction.DefaultAppChanged -> isDefaultApp.value = action.isDefault
            is ContactsAction.DeleteContacts -> viewModelScope.launch(Dispatchers.IO) {
                contactsRepository.deleteContacts(action.ids)
                    .onSuccess {
                        contactSettingsDao.deleteContactsSettings(action.ids)
                    }
                    .onFailure { e ->
                        // Still clean up settings of the contacts that did get deleted
                        if (e is ContactsDeletionException) {
                            val deletedIds = action.ids - e.failedIds.toSet()
                            if (deletedIds.isNotEmpty()) {
                                contactSettingsDao.deleteContactsSettings(deletedIds)
                            }
                        }
                        _events.send(ContactsEvent.DeleteError)
                    }
            }

            is ContactsAction.ChangeLabelFiltering -> labelFilter.value = action.label
            is ContactsAction.ToggleFavorite -> viewModelScope.launch(Dispatchers.IO) {
                contactsRepository.toggleFavorite(action.contacts)
            }
        }
    }

    companion object {
        private const val ALL_ACCOUNTS = "All"
    }
}

@Stable
data class ContactsState(
    val isLoading: Boolean = false,
    /**
     * Favorite contacts aren't paged, the others are in [ContactsViewModel.contacts]
     */
    val favorites: List<CuteContact> = emptyList(),
    val accountsToCount: Map<String, Int> = emptyMap(),
    val accountFilter: String? = null,
    val labelsToCount: Map<String, Int> = emptyMap(),
    val labelFilter: String? = null
)

sealed interface ContactsEvent {
    data object DeleteError : ContactsEvent
}

sealed interface ContactsAction {
    data class ChangeAccountFiltering(val accountName: String) : ContactsAction
    data class DeleteContacts(val ids: List<Long>) : ContactsAction
    data class ToggleFavorite(val contacts: List<CuteContact>) : ContactsAction
    data class DefaultAppChanged(val isDefault: Boolean) : ContactsAction
    data class ChangeLabelFiltering(val label: String?) : ContactsAction
}
