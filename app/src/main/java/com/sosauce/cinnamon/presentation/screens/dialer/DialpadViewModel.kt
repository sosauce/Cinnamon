package com.sosauce.cinnamon.presentation.screens.dialer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DialpadViewModel(
    private val contactsRepository: ContactsRepository
): ViewModel() {

    private val _state = MutableStateFlow(DialpadState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepository.fetchLatestContacts().collectLatest { contacts ->
                _state.update {
                    it.copy(
                        contacts = contacts,
                        isLoading = false
                    )
                }
            }
        }
    }

}

data class DialpadState(
    val isLoading: Boolean = false,
    val contacts: List<CuteContact> = emptyList()
)