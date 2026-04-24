@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.presentation.screens.contacts

import android.accounts.Account
import android.provider.ContactsContract
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.datastore.UserPreferences
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.repository.ContactsRepository
import com.sosauce.cinnamon.utils.copyMutate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences
): ViewModel() {

    private val _state = MutableStateFlow(ContactsState(isLoading = true))
    val state = _state.asStateFlow()

    private val textFieldState = TextFieldState()


    init {
        viewModelScope.launch(Dispatchers.IO) {

            combine(
                contactsRepository.fetchLatestContacts(),
                snapshotFlow { textFieldState.text }.debounce(250),
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
                        else it.accountType == accountFilter
                    }.copyMutate {
                        if (!asc) reverse()
                    }
            }.flowOn(Dispatchers.Default).collectLatest { contacts ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        contacts = contacts,
                        textFieldState = textFieldState
                    )
                }
            }

        }

        viewModelScope.launch(Dispatchers.IO) {
            val accounts = contactsRepository.fetchAccounts()
            _state.update {
                it.copy(
                    contactAccounts = accounts
                )
            }
        }
    }


    fun handleContactsAction(action: ContactsAction) {
        when(action) {
            is ContactsAction.ChangeAccountFiltering -> {
                _state.update {
                    it.copy(
                        accountFilter = action.accountType
                    )
                }
            }
        }
    }

    companion object {
        private val contactDataSearch = listOf(
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
            ContactsContract.CommonDataKinds.Note.NOTE,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        )

        const val ACCOUNT_FILTER_DEFAULT = "all"

    }

}

data class ContactsState(
    val isLoading: Boolean = false,
    val contacts: List<CuteContact> = emptyList(),
    val contactAccounts: List<Account> = emptyList(),
    val textFieldState: TextFieldState = TextFieldState(),
    val accountFilter: String = ContactsViewModel.ACCOUNT_FILTER_DEFAULT
)

sealed interface ContactsAction {
    data class ChangeAccountFiltering(val accountType: String) : ContactsAction
}