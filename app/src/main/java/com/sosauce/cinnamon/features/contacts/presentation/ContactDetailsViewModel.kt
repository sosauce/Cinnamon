package com.sosauce.cinnamon.features.contacts.presentation

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.incall.CallLauncher
import com.sosauce.cinnamon.features.incall.LaunchCallResult
import com.sosauce.cinnamon.core.utils.unblock
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsEntity
import com.sosauce.cinnamon.features.contacts.data.repository.ContactGroupsRepository
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import com.sosauce.cinnamon.features.contacts.domain.CuteRawContact
import com.sosauce.cinnamon.features.phone.domain.SimChoice
import com.sosauce.cinnamon.features.phone.domain.SimPickerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ContactDetailsViewModel(
    private val application: Application,
    private val contactId: Long,
    private val contactsRepository: ContactsRepository,
    private val contactSettingsDao: ContactSettingsDao,
    private val callLauncher: CallLauncher,
    private val contactGroupsRepository: ContactGroupsRepository
) : AndroidViewModel(application) {


    private val _events = Channel<ContactDetailsEvent>()
    val events = _events.receiveAsFlow()

    private val _simPicker = MutableStateFlow<SimPickerState?>(null)
    val simPicker = _simPicker.asStateFlow()

    private val details = contactsRepository.fetchLatestContactsDetails(contactId)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)
    private val labels = combine(
        contactGroupsRepository.observeGroups(),
        contactGroupsRepository.observeMemberships(contactId)
    ) { groups, memberships ->
        groups.filter { it.id in memberships }.map { it.title }.distinct()
    }
    private val _state = MutableStateFlow(ContactDetailsState())
    val state = combine(
        _state,
        contactsRepository.fetchContact(contactId),
        details,
        contactSettingsDao.getContactSettings(contactId),
        labels
    ) { currentState, contact, details, settings, labels ->
        currentState.copy(
            isLoading = false,
            contact = contact,
            details = details,
            settings = settings ?: ContactSettingsEntity(contactId = contactId),
            labels = labels
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
                    val settings = state.value.settings

                    contactsRepository.deleteContacts(listOf(contactId))
                        .onSuccess {
                            try {
                                if (settings.poster.isNotBlank()) {
                                    File(application.filesDir, settings.poster).delete()
                                }
                                contactSettingsDao.deleteContactSettings(settings)
                            } catch (e: Exception) {
                                // The contact itself is gone, leftover local settings shouldn't count as a failure
                                if (e is CancellationException) ensureActive()
                                e.printStackTrace()
                            }
                            _events.send(ContactDetailsEvent.Success)
                        }
                        .onFailure {
                            _events.send(ContactDetailsEvent.Error)
                        }
                }
            }

            is ContactDetailsAction.BlockContact -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val contact = state.value.contact
                    val emails =
                        if (action.emailsToo) state.value.details.emails.fastMap { it.email } else emptyList()
                    contactsRepository.blockContact(
                        phones = contact.phoneNumbers.fastMap { it.number },
                        emails = emails
                    )
                }
            }

            is ContactDetailsAction.Unblock -> {
                viewModelScope.launch(Dispatchers.IO) {
                    application.unblock(action.value)
                }
            }

            is ContactDetailsAction.CallNumber -> {
                viewModelScope.launch {
                    when (val result = callLauncher.launchCall(action.number)) {
                        LaunchCallResult.NotAllowed -> _events.send(ContactDetailsEvent.RequestDefaultDialer)
                        is LaunchCallResult.NeedsSimSelection -> _simPicker.update {
                            SimPickerState(action.number, result.choices)
                        }

                        else -> Unit
                    }
                }
            }

            is ContactDetailsAction.PickSim -> {
                val number = _simPicker.value?.number ?: return
                _simPicker.update { null }
                viewModelScope.launch {
                    if (callLauncher.launchCall(number, action.sim.handle) == LaunchCallResult.NotAllowed) {
                        _events.send(ContactDetailsEvent.RequestDefaultDialer)
                    }
                }
            }

            is ContactDetailsAction.DismissSimPicker -> _simPicker.update { null }

            is ContactDetailsAction.SetRingtone -> {
                viewModelScope.launch {
                    if (!contactsRepository.setContactRingtone(contactId, action.uri)) {
                        _events.send(ContactDetailsEvent.RingtoneError)
                    }
                }
            }

            is ContactDetailsAction.SetSendToVoicemail -> {
                viewModelScope.launch {
                    if (!contactsRepository.setSendToVoicemail(contactId, action.enabled)) {
                        _events.send(ContactDetailsEvent.SendToVoicemailError)
                    }
                }
            }

            is ContactDetailsAction.RingtonePickerUnavailable -> {
                viewModelScope.launch { _events.send(ContactDetailsEvent.RingtoneError) }
            }
        }
    }

}

@Stable
data class ContactDetailsState(
    val isLoading: Boolean = false,
    val contact: CuteContact = CuteContact(),
    val rawContacts: List<CuteRawContact> = emptyList(),
    val details: CuteContactDetails = CuteContactDetails(),
    val settings: ContactSettingsEntity = ContactSettingsEntity(),
    val labels: List<String> = emptyList()
)

sealed interface ContactDetailsEvent {
    data object Error : ContactDetailsEvent
    data object Success : ContactDetailsEvent

    /**
     * A call couldn't be launched because Cinnamon isn't the default dialer app
     */
    data object RequestDefaultDialer : ContactDetailsEvent

    /**
     * The contact's ringtone couldn't be saved
     */
    data object RingtoneError : ContactDetailsEvent

    data object SendToVoicemailError : ContactDetailsEvent
}

sealed interface ContactDetailsAction {
    data object ToggleFavorite : ContactDetailsAction
    data object ShareContact : ContactDetailsAction
    data object DeleteContact : ContactDetailsAction
    data class BlockContact(val emailsToo: Boolean) : ContactDetailsAction
    data class Unblock(val value: String) : ContactDetailsAction
    data class CallNumber(val number: String) : ContactDetailsAction
    data class PickSim(val sim: SimChoice) : ContactDetailsAction
    data object DismissSimPicker : ContactDetailsAction

    /**
     * @param uri the ringtone picker's result, null or the default ringtone uri resets it
     */
    data class SetRingtone(val uri: Uri?) : ContactDetailsAction

    /**
     * No app can handle the ringtone picker intent
     */
    data object RingtonePickerUnavailable : ContactDetailsAction

    data class SetSendToVoicemail(val enabled: Boolean) : ContactDetailsAction
}
