package com.sosauce.cinnamon.presentation.screens.contacts

import android.app.Application
import android.provider.BlockedNumberContract
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.contact_settings.ContactSettings
import com.sosauce.cinnamon.data.contact_settings.ContactSettingsDao
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.repository.ContactsRepository
import com.sosauce.cinnamon.utils.blockNumbers
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ContactDetailsViewModel(
    private val application: Application,
    private val contactId: Long,
    private val contactsRepository: ContactsRepository,
    private val contactSettingsDao: ContactSettingsDao
) : AndroidViewModel(application) {


    private val _state = MutableStateFlow(ContactDetailsState(isLoading = true))
    val state = _state.asStateFlow()


    init {

        viewModelScope.launch(Dispatchers.IO) {
            contactsRepository.fetchLatestContactsDetails(contactId).collectLatest { contact ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        contact = contact
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            contactSettingsDao.getContactSettings(contactId).collectLatest { settings ->
                _state.update {
                    it.copy(settings = settings ?: ContactSettings(contactId = contactId))
                }
            }
        }

        application.contentResolver.observe(BlockedNumberContract.BlockedNumbers.CONTENT_URI)
            .onEach {
                _state.update { state ->

                    val contact = state.contact

                    val updatedPhones = contact.details.phoneNumbers.fastMap { phone ->
                        phone.copy(
                            isBlocked = BlockedNumberContract.isBlocked(application, phone.number)
                        )
                    }

                    val updatedEmails = contact.details.emails.fastMap { email ->
                        email.copy(
                            isBlocked = BlockedNumberContract.isBlocked(application, email.email)
                        )
                    }

                    state.copy(
                        contact = contact.copy(
                            details = contact.details.copy(
                                phoneNumbers = updatedPhones,
                                emails = updatedEmails
                            )
                        )
                    )
                }
            }.flowOn(Dispatchers.Default).launchIn(viewModelScope)


    }

    fun handleContactDetailsAction(action: ContactDetailsAction) {
        when (action) {
            is ContactDetailsAction.ToggleFavorite -> {
                viewModelScope.launch {
                    contactsRepository.toggleFavorite(listOf(state.value.contact))
                }
            }

            is ContactDetailsAction.ShareContact -> {}
            is ContactDetailsAction.DeleteContact -> {
                viewModelScope.launch(Dispatchers.IO) {
                    contactsRepository.deleteContacts(listOf(contactId))

                    // Delete poster before deleting settings from local db or else we might lose reference to the poster in the state before being able to delete
                    File(application.filesDir, state.value.settings.poster).delete()
                    contactSettingsDao.deleteContactSettings(state.value.settings)
                }
            }

            is ContactDetailsAction.BlockContact -> {
                val toBlock = if (action.emailsToo) {
                    state.value.contact.details.phoneNumbers.fastMap { it.number } + state.value.contact.details.emails.fastMap { it.email }
                } else state.value.contact.details.phoneNumbers.fastMap { it.number }

                viewModelScope.launch(Dispatchers.IO) {
                    application.blockNumbers(toBlock)
                }
            }
        }
    }

}

data class ContactDetailsState(
    val isLoading: Boolean = false,
    val contact: CuteContact = CuteContact(),
    val settings: ContactSettings = ContactSettings(),
)

sealed interface ContactDetailsAction {
    data object ToggleFavorite : ContactDetailsAction
    data object ShareContact : ContactDetailsAction
    data object DeleteContact : ContactDetailsAction
    data class BlockContact(val emailsToo: Boolean) : ContactDetailsAction
}