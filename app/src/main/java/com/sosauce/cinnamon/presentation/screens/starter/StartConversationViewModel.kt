@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.presentation.screens.starter

import android.provider.ContactsContract
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.request.Disposable
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartConversationViewModel(
    private val contactsRepository: ContactsRepository
): ViewModel() {

    private val _state = MutableStateFlow(StartConversationState())
    val state = _state.asStateFlow()

    private val textFieldState = TextFieldState()

    init {
        viewModelScope.launch(Dispatchers.IO) {

            combine(
                contactsRepository.fetchLatestContacts(
                    extraSelection = "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > ?",
                    extraSelectionArgs = arrayOf("0")
                ),
                snapshotFlow { textFieldState.text }.debounce(250)
            ) { contacts, searchQuery ->
                contacts
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
                        contacts = contacts,
                        textFieldState = textFieldState
                    )
                }
            }
        }
    }
}


data class StartConversationState(
    val contacts: List<CuteContact> = emptyList(),
    val isGroupChatMode: Boolean = false,
    val selectedNumbers: List<String> = emptyList(),
    val textFieldState: TextFieldState = TextFieldState(),
    val isSearching: Boolean = textFieldState.text.isNotEmpty()
)

sealed interface StartConversationActions {
    data object ToggleGroupMode : StartConversationActions
}