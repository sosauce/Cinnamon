package com.sosauce.cinnamon.presentation.screens.contacts

import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.contact_settings.ContactSettings
import com.sosauce.cinnamon.data.contact_settings.ContactSettingsActions
import com.sosauce.cinnamon.data.contact_settings.ContactSettingsDao
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactDetailsViewModel(
    private val contactId: Long,
    private val contactsRepository: ContactsRepository,
    private val contactSettingsDao: ContactSettingsDao
): ViewModel() {


    private val _state = MutableStateFlow(ContactDetailsState(isLoading = true))
    val state = _state.asStateFlow()


    init {

        viewModelScope.launch(Dispatchers.IO) {
            contactsRepository.fetchLatestContacts(
                extraSelection = "${ContactsContract.Contacts._ID} = ?",
                extraSelectionArgs = arrayOf(contactId.toString())
            ).collectLatest { contacts ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        contact = contacts.firstOrNull() ?: CuteContact(id = contactId)
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            contactSettingsDao.getConversationSettings(contactId).collectLatest { settings ->
                _state.update {
                    it.copy(settings = settings ?: ContactSettings(contactId.toInt()))
                }
            }
        }
    }

    fun handleContactDetailsAction(action: ContactDetailsAction) {
        when(action) {
            is ContactDetailsAction.ToggleFavorite -> {
                viewModelScope.launch {
                    contactsRepository.toggleFavorite(listOf(state.value.contact))
                }
            }
            is ContactDetailsAction.ShareContact -> {}
            is ContactDetailsAction.DeleteContact -> {
                viewModelScope.launch {
                    contactsRepository.deleteContacts(listOf(contactId))
                }
            }
        }
    }

}

data class ContactDetailsState(
    val isLoading: Boolean = false,
    val contact: CuteContact = CuteContact(),
    val settings: ContactSettings = ContactSettings()
)

sealed interface ContactDetailsAction {
    data object ToggleFavorite : ContactDetailsAction
    data object ShareContact : ContactDetailsAction
    data object DeleteContact : ContactDetailsAction
}