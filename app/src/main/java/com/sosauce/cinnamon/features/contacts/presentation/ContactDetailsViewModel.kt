package com.sosauce.cinnamon.features.contacts.presentation

import android.app.Application
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsEntity
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ContactDetailsViewModel(
    private val application: Application,
    private val contactId: Long,
    private val contactsRepository: ContactsRepository,
    private val contactSettingsDao: ContactSettingsDao
) : AndroidViewModel(application) {




    val state = combine(
        contactsRepository.fetchContact2(contactId),
        contactsRepository.fetchLatestContactsDetails(contactId),
        contactSettingsDao.getContactSettings(contactId)
    ) { contact, details, settings ->
        ContactDetailsState(
            isLoading = false,
            contact = contact,
            details = details,
            settings = settings ?: ContactSettingsEntity(contactId = contactId)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ContactDetailsState(isLoading = true)
    )


    init {

//        application.contentResolver.observe(BlockedNumberContract.BlockedNumbers.CONTENT_URI)
//            .onEach {
//                _state.update { state ->
//
//                    val contact = state.contact
//
//                    val updatedPhones = contact.details.phoneNumbers.fastMap { phone ->
//                        phone.copy(
//                            isBlocked = BlockedNumberContract.isBlocked(application, phone.number)
//                        )
//                    }
//
//                    val updatedEmails = contact.details.emails.fastMap { email ->
//                        email.copy(
//                            isBlocked = BlockedNumberContract.isBlocked(application, email.email)
//                        )
//                    }
//
//                    state.copy(
//                        contact = contact.copy(
//                            details = contact.details.copy(
//                                phoneNumbers = updatedPhones,
//                                emails = updatedEmails
//                            )
//                        )
//                    )
//                }
//            }.flowOn(Dispatchers.Default).launchIn(viewModelScope)


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
                viewModelScope.launch(Dispatchers.IO) {
                    val contact = state.value.contact
                    val emails = if (action.emailsToo) state.value.details.emails.fastMap { it.email } else emptyList()
                    contactsRepository.blockContact(
                        phones = contact.phoneNumbers.fastMap { it.number },
                        emails = emails
                    )
                }
            }
        }
    }

}

data class ContactDetailsState(
    val isLoading: Boolean = false,
    val contact: CuteContact = CuteContact(),
    val details: CuteContactDetails = CuteContactDetails(),
    val settings: ContactSettingsEntity = ContactSettingsEntity(),
)

sealed interface ContactDetailsAction {
    data object ToggleFavorite : ContactDetailsAction
    data object ShareContact : ContactDetailsAction
    data object DeleteContact : ContactDetailsAction
    data class BlockContact(val emailsToo: Boolean) : ContactDetailsAction
}