@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.presentation.dialer

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
import com.sosauce.cinnamon.features.contacts.presentation.toPhoneContactsSelection
import com.sosauce.cinnamon.features.incall.CallLauncher
import com.sosauce.cinnamon.features.incall.LaunchCallResult
import com.sosauce.cinnamon.features.phone.domain.SimChoice
import com.sosauce.cinnamon.features.phone.domain.SimPickerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DialerViewModel(
    private val prefilledNumber: String,
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences,
    private val callLauncher: CallLauncher
) : ViewModel() {

    val textFieldState = TextFieldState(prefilledNumber)

    private val _events = Channel<DialerEvent>()
    val events = _events.receiveAsFlow()

    private val _simPicker = MutableStateFlow<SimPickerState?>(null)
    val simPicker = _simPicker.asStateFlow()

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
        selection = searchResults.map { it.toPhoneContactsSelection() },
        sortAscending = flowOf(true)
    )

    /**
     * Non favorite contacts, paged, with a letter header before each group.
     */
    val contacts: Flow<PagingData<ContactsListItem>> = pager.contacts

    val state = combine(
        pager.favorites,
        pager.loaded,
        userPreferences.speedDials
    ) { favorites, loaded, speedDials ->
        DialerState(
            isLoading = !loaded || favorites == null,
            favorites = favorites.orEmpty(),
            speedDials = speedDials
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DialerState(isLoading = true)
    )

    fun handleDialerAction(action: DialerAction) {
        when (action) {
            is DialerAction.CallNumber -> {
                viewModelScope.launch {
                    when (val result = callLauncher.launchCall(action.number)) {
                        LaunchCallResult.NotAllowed -> _events.send(DialerEvent.RequestDefaultDialer)
                        is LaunchCallResult.NeedsSimSelection -> _simPicker.update {
                            SimPickerState(action.number, result.choices)
                        }

                        else -> Unit
                    }
                }
            }

            is DialerAction.PickSim -> {
                val number = _simPicker.value?.number ?: return
                _simPicker.update { null }
                viewModelScope.launch {
                    if (callLauncher.launchCall(number, action.sim.handle) == LaunchCallResult.NotAllowed) {
                        _events.send(DialerEvent.RequestDefaultDialer)
                    }
                }
            }

            is DialerAction.DismissSimPicker -> _simPicker.update { null }
        }
    }
}

@Stable
data class DialerState(
    val isLoading: Boolean = false,
    /**
     * Favorite contacts aren't paged, the others are in [DialerViewModel.contacts]
     */
    val favorites: List<CuteContact> = emptyList(),
    val speedDials: Map<Int, String> = emptyMap()
)

sealed interface DialerAction {
    data class CallNumber(val number: String) : DialerAction
    data class PickSim(val sim: SimChoice) : DialerAction
    data object DismissSimPicker : DialerAction
}

sealed interface DialerEvent {
    /**
     * The call couldn't be launched because Cinnamon isn't the default dialer app
     */
    data object RequestDefaultDialer : DialerEvent
}
