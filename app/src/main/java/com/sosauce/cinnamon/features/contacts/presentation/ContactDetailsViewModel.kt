package com.sosauce.cinnamon.features.contacts.presentation

import android.app.Application
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsEntity
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import com.sosauce.cinnamon.features.contacts.domain.CuteRawContact
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ContactDetailsViewModel(
    private val application: Application,
    private val contactId: Long,
    private val contactsRepository: ContactsRepository,
    private val contactSettingsDao: ContactSettingsDao,
    private val callManager: CallManager
) : AndroidViewModel(application) {



    private val _events = Channel<ContactDetailsEvent>()
    val events = _events.receiveAsFlow()

    private val _state = MutableStateFlow(ContactDetailsState())
    val state = combine(
        _state,
        contactsRepository.fetchContact(contactId),
        contactsRepository.fetchLatestContactsDetails(contactId),
        contactSettingsDao.getContactSettings(contactId)
    ) { currentState, contact, details, settings ->
        currentState.copy(
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
        viewModelScope.launch {
            _state.update {
                it.copy(
                    rawContacts = contactsRepository.fetchContactRawContacts(contactId)
                )
            }
        }
    }

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
                    try {
                        contactsRepository.deleteContacts(listOf(contactId))

                        // Delete poster before deleting settings from local db or else we might lose reference to the poster in the state before being able to delete
                        File(application.filesDir, state.value.settings.poster).delete()
                        contactSettingsDao.deleteContactSettings(state.value.settings)

                        _events.send(ContactDetailsEvent.Success)
                    } catch (e: Exception) {
                        if (e is CancellationException) ensureActive()
                        _events.send(ContactDetailsEvent.Error)
                    }


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
            is ContactDetailsAction.CallNumber -> {
                callManager.startCall(action.number)
            }
        }
    }

}

data class ContactDetailsState(
    val isLoading: Boolean = false,
    val contact: CuteContact = CuteContact(),
    val rawContacts: List<CuteRawContact> = emptyList(),
    val details: CuteContactDetails = CuteContactDetails(),
    val settings: ContactSettingsEntity = ContactSettingsEntity(),
)

sealed interface ContactDetailsEvent {
    data object Error : ContactDetailsEvent
    data object Success : ContactDetailsEvent
}

sealed interface ContactDetailsAction {
    data object ToggleFavorite : ContactDetailsAction
    data object ShareContact : ContactDetailsAction
    data object DeleteContact : ContactDetailsAction
    data class BlockContact(val emailsToo: Boolean) : ContactDetailsAction
    data class CallNumber(val number: String) : ContactDetailsAction
}