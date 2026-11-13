@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.presentation.dialpad

import android.provider.ContactsContract
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.presentation.ContactsListItem
import com.sosauce.cinnamon.features.contacts.presentation.ContactsPager
import com.sosauce.cinnamon.features.contacts.presentation.toContactIdSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

class DialpadViewModel(
    private val prefilledNumber: String,
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val textFieldState = TextFieldState(prefilledNumber)

    private val searchQuery = snapshotFlow { textFieldState.text.toString() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()

    /**
     * Ids of the contacts matching the typed number (or its T9 name), null when nothing is typed
     */
    private val searchResults: Flow<Set<Long>?> = combine(
        searchQuery,
        userPreferences.enableT9Dialing
    ) { query, t9 -> query to t9 }
        .distinctUntilChanged()
        .flatMapLatest { (query, t9) ->
            if (query.isEmpty()) {
                flowOf(null)
            } else {
                // refresh results when contacts change
                contactsRepository
                    .observeContactChanges()
                    .mapLatest { contactsRepository.searchDialpadContactIds(query, t9) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val pager = ContactsPager(
        contactsRepository = contactsRepository,
        scope = viewModelScope,
        // only contacts that can be called
        selection = searchResults.map { results ->
            listOfNotNull(
                "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
                results?.toContactIdSelection()
            ).joinToString(" AND ")
        },
        sortAscending = flowOf(true)
    )

    /**
     * Non favorite contacts, paged, with a letter header before each group.
     */
    val contacts: Flow<PagingData<ContactsListItem>> = pager.contacts

    val state = combine(
        pager.favorites,
        pager.loaded
    ) { favorites, loaded ->
        DialpadState(
            isLoading = !loaded || favorites == null,
            favorites = favorites.orEmpty()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DialpadState(isLoading = true)
    )
}

@Stable
data class DialpadState(
    val isLoading: Boolean = false,
    /**
     * Favorite contacts aren't paged, the others are in [DialpadViewModel.contacts]
     */
    val favorites: List<CuteContact> = emptyList()
)
