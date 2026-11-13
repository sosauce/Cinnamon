@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.starter

import android.provider.ContactsContract
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sosauce.cinnamon.core.utils.copyMutate
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.presentation.ContactsListItem
import com.sosauce.cinnamon.features.contacts.presentation.ContactsPager
import com.sosauce.cinnamon.features.contacts.presentation.toContactIdSelection
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

class StartConversationViewModel(
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StartConversationState())

    val textFieldState = TextFieldState()

    private val searchQuery = snapshotFlow { textFieldState.text.toString().trim() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()

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

    private val pager = ContactsPager(
        contactsRepository = contactsRepository,
        scope = viewModelScope,
        // only contacts that can be texted
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
        _state,
        pager.favorites,
        pager.loaded
    ) { state, favorites, loaded ->
        state.copy(
            isLoading = !loaded || favorites == null,
            favorites = favorites.orEmpty()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StartConversationState(isLoading = true)
    )


    fun toggleGroupChatMode() {
        _state.update {
            it.copy(
                isGroupChatMode = !it.isGroupChatMode,
                selectedNumbers = emptyList()
            )
        }
    }

    fun addNumberToGroup(number: String) {
        _state.update {
            it.copy(
                selectedNumbers = it.selectedNumbers.copyMutate {
                    if (!remove(number)) {
                        add(number)
                    }
                }
            )
        }
    }
}


@Stable
data class StartConversationState(
    val isLoading: Boolean = false,
    /**
     * Favorite contacts aren't paged, the others are in [StartConversationViewModel.contacts]
     */
    val favorites: List<CuteContact> = emptyList(),
    val isGroupChatMode: Boolean = false,
    val selectedNumbers: List<String> = emptyList()
)

sealed interface StartConversationActions {
    data object ToggleGroupMode : StartConversationActions
}
