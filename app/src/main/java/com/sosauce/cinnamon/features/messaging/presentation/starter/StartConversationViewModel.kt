@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.starter

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.core.utils.copyMutate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class StartConversationViewModel(
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StartConversationState())
    val state = _state.asStateFlow()

    val textFieldState = TextFieldState()

    init {
        viewModelScope.launch(Dispatchers.IO) {

            combine(
                contactsRepository.fetchLatestContacts(),
                snapshotFlow { textFieldState.text }.debounce(250.milliseconds)
            ) { contacts, searchQuery ->
                contacts
                    .fastFilter { it.phoneNumbers.isNotEmpty() }
                    .fastFilter {
                        if (searchQuery.isEmpty()) {
                            true
                        } else {
                            it.searchIndex.contains(searchQuery, true)
                        }
                    }
            }.collectLatest { contacts ->
                _state.update {
                    it.copy(
                        contacts = contacts
                    )
                }
            }
        }
    }


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


data class StartConversationState(
    val isLoading: Boolean = false,
    val contacts: List<CuteContact> = emptyList(),
    val isGroupChatMode: Boolean = false,
    val selectedNumbers: List<String> = emptyList()
)

sealed interface StartConversationActions {
    data object ToggleGroupMode : StartConversationActions
}
