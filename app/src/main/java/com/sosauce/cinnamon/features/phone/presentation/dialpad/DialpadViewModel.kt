@file:OptIn(FlowPreview::class)

package com.sosauce.cinnamon.features.phone.presentation.dialpad

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

class DialpadViewModel(
    private val prefilledNumber: String,
    private val contactsRepository: ContactsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val textFieldState = TextFieldState(prefilledNumber)
    val state = combine(
        userPreferences.enableT9Dialing,
        snapshotFlow { textFieldState.text }.debounce(250.milliseconds)
    ) { t9, searchQuery ->

        val contacts = contactsRepository.fetchDialpadContacts()

        val filteredContacts = if (t9) {
            contacts.fastFilter { contact ->
                nameToT9(contact.displayName).contains(searchQuery, true) ||
                        contact.phoneNumbers.fastAny {
                            it.number.contains(
                                searchQuery
                            )
                        }
            }
        } else {
            contacts.fastFilter { contact ->
                contact.phoneNumbers.fastAny { it.number.contains(searchQuery) }
            }
        }

        DialpadState(
            isLoading = false,
            contacts = filteredContacts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DialpadState(
            isLoading = true
        )
    )



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
    val contacts: List<CuteContact> = emptyList()
)