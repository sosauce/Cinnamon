@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import android.app.Application
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.data.model.CuteContact
import com.sosauce.cinnamon.core.utils.copyMutate
import com.sosauce.cinnamon.features.contacts.domain.CuteContact2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ContactsViewModel(
    private val application: Application,
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences,
    private val contactSettingsDao: ContactSettingsDao
) : AndroidViewModel(application) {

    val textFieldState = TextFieldState()
    private val _state = MutableStateFlow(
        ContactsState(
            isLoading = true
        )
    )

    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            combine(
                contactsRepository.fetchLatestContacts2(),
                snapshotFlow { textFieldState.text }.debounce(250.milliseconds),
                userPreferences.getSortContactsAscending(),
                state.mapLatest { it.accountFilter }.distinctUntilChanged()
            ) { contacts, search, asc, accountFilter ->
                contacts
                    .fastFilter {
                        if (search.isEmpty()) {
                            true
                        } else {
                            it.searchIndex.contains(search, true)
                        }
                    }
                    .fastFilter {
                        if (accountFilter == ACCOUNT_FILTER_DEFAULT) true
                        else it.accountName == accountFilter
                    }.apply {
                        if (!asc) reversed()
                    }
            }.flowOn(Dispatchers.Default).collectLatest { contacts ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        contacts = contacts
                    )
                }
            }
        }

        viewModelScope.launch {
            contactsRepository.fetchLatestContacts().collectLatest { contacts ->
                val accountsToCount =
                    mapOf("All" to contacts.size) + contacts.groupingBy { it.accountName ?: "IDK" }
                        .eachCount()

                _state.update {
                    it.copy(
                        accountsToCount = accountsToCount
                    )
                }
            }
        }
    }


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

    companion object {
        const val ACCOUNT_FILTER_DEFAULT = "All"
    }

}

data class ContactsState(
    val isLoading: Boolean = false,
    val contacts: List<CuteContact2> = emptyList(),
    val accountsToCount: Map<String, Int> = emptyMap(),
    val accountFilter: String = ContactsViewModel.ACCOUNT_FILTER_DEFAULT
)

sealed interface ContactsAction {
    data class ChangeAccountFiltering(val accountName: String) : ContactsAction
    data class DeleteContacts(
        val ids: List<Long>
    ) : ContactsAction

    data class ToggleFavorite(val contacts: List<CuteContact2>) : ContactsAction
}