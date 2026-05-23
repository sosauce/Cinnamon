@file:OptIn(FlowPreview::class)

package com.sosauce.cinnamon.presentation.screens.dialer

import android.provider.ContactsContract
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DialpadViewModel(
    private val prefilledNumber: String,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DialpadState())
    val state = _state.asStateFlow()

    private val textFieldState = TextFieldState(prefilledNumber)

    init {
        viewModelScope.launch(Dispatchers.IO) {

            combine(
                contactsRepository.fetchLatestContacts(
                    extraSelection = "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = ?",
                    extraSelectionArgs = arrayOf("1")
                ),
                snapshotFlow { textFieldState.text }.debounce(250)
            ) { contacts, searchQuery ->

                contacts.fastFilter { contact ->
                    nameToT9(contact.displayName).contains(searchQuery, true) ||
                            contact.details.phoneNumbers.fastAny { it.number.contains(searchQuery) }
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


    fun addPlus() = textFieldState.edit { insert(length, "+") }


    private fun nameToT9(name: String): String {
        return name.lowercase().map {
            when (it) {
                'a', 'b', 'c' -> '2'
                'd', 'e', 'f' -> '3'
                'g', 'h', 'i' -> '4'
                'j', 'k', 'l' -> '5'
                'm', 'n', 'o' -> '6'
                'p', 'q', 'r', 's' -> '7'
                't', 'u', 'v' -> '8'
                'w', 'x', 'y', 'z' -> '9'
                '+' -> '+'
                else -> '0'
            }
        }.joinToString("")
    }

}

data class DialpadState(
    val isLoading: Boolean = false,
    val contacts: List<CuteContact> = emptyList(),
    val textFieldState: TextFieldState = TextFieldState()
)