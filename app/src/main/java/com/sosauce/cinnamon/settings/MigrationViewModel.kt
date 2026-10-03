package com.sosauce.cinnamon.settings

import android.app.Application
import android.net.Uri
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.contacts.data.backup.ContactsBackupRepository
import com.sosauce.cinnamon.features.contacts.data.backup.ContactsImportRepository
import com.sosauce.cinnamon.features.contacts.data.backup.ImportableContact
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.messaging.data.backup.BackupConversation
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import com.sosauce.cinnamon.features.messaging.data.backup.MessageBackupRepository
import com.sosauce.cinnamon.features.messaging.data.backup.SmsImportRepository
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessageBackupUiState(
    val fileName: String = "",
    val selectedThreadIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    val progress: Float? = null,
    val progressLabel: String? = null
)

data class ContactsBackupUiState(
    val fileName: String = "",
    val selectedContactIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    val progress: Float? = null,
    val progressLabel: String? = null
)

class MigrationViewModel(
    private val application: Application,
    private val contactsRepository: ContactsRepository,
    private val conversationsRepository: ConversationsRepository,
    private val messageBackupRepository: MessageBackupRepository,
    private val contactsBackupRepository: ContactsBackupRepository,
    private val smsImportRepository: SmsImportRepository,
    private val contactsImportRepository: ContactsImportRepository
) : AndroidViewModel(application) {

    val backupConversations: StateFlow<List<CuteConversation>> =
        conversationsRepository.fetchLatestConversations()
            .map { entities -> entities.fastMap { it.toCuteConversation() } }
            .catch { emit(emptyList()) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val backupContacts: StateFlow<List<CuteContact>> =
        contactsRepository.fetchLatestContacts()
            .catch { emit(emptyList()) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    private val _backupUiState = MutableStateFlow(
        MessageBackupUiState(fileName = messageBackupRepository.defaultFileName())
    )
    val backupUiState: StateFlow<MessageBackupUiState> = _backupUiState.asStateFlow()

    private val _contactsBackupUiState = MutableStateFlow(
        ContactsBackupUiState(fileName = contactsBackupRepository.defaultFileName())
    )
    val contactsBackupUiState: StateFlow<ContactsBackupUiState> =
        _contactsBackupUiState.asStateFlow()

    private val _smsImportSession = MutableStateFlow<SmsImportSession?>(null)
    val smsImportSession: StateFlow<SmsImportSession?> = _smsImportSession.asStateFlow()

    private val _contactsImportSession = MutableStateFlow<ContactsImportSession?>(null)
    val contactsImportSession: StateFlow<ContactsImportSession?> =
        _contactsImportSession.asStateFlow()

    private val _events = Channel<MigrationEvent>()
    val events = _events.receiveAsFlow()

    private var smsSelectionInitialized = false
    private var contactsSelectionInitialized = false

    init {
        // Default to select-all on first load so Export works immediately.
        // Users can still deselect individual items afterwards.
        viewModelScope.launch {
            backupConversations.collect { conversations ->
                if (!smsSelectionInitialized && conversations.isNotEmpty()) {
                    smsSelectionInitialized = true
                    _backupUiState.update {
                        it.copy(selectedThreadIds = conversations.fastMap { c -> c.threadId }
                            .toSet())
                    }
                }
            }
        }
        viewModelScope.launch {
            backupContacts.collect { contacts ->
                if (!contactsSelectionInitialized && contacts.isNotEmpty()) {
                    contactsSelectionInitialized = true
                    _contactsBackupUiState.update {
                        it.copy(selectedContactIds = contacts.fastMap { c -> c.id }.toSet())
                    }
                }
            }
        }
    }

    fun toggleConversationSelected(threadId: Long, selected: Boolean) {
        _backupUiState.update { state ->
            val updated = if (selected) {
                state.selectedThreadIds + threadId
            } else {
                state.selectedThreadIds - threadId
            }
            state.copy(selectedThreadIds = updated)
        }
    }

    fun selectAllConversations(threadIds: List<Long>) {
        _backupUiState.update {
            it.copy(selectedThreadIds = threadIds.toSet())
        }
    }

    fun clearConversationSelection() {
        _backupUiState.update {
            it.copy(selectedThreadIds = emptySet())
        }
    }

    fun onBackupFileNameChange(name: String) {
        _backupUiState.update { it.copy(fileName = name) }
    }

    fun sanitizedSmsFileName(): String {
        return messageBackupRepository.sanitizeFileName(_backupUiState.value.fileName)
    }

    fun exportSmsBackup(destination: Uri) {
        if (_backupUiState.value.isExporting) return

        viewModelScope.launch {
            val snapshot = _backupUiState.value
            if (snapshot.selectedThreadIds.isEmpty()) {
                _events.send(
                    MigrationEvent.SmsExportError(
                        application.getString(R.string.backup_select_at_least_one)
                    )
                )
                return@launch
            }

            _backupUiState.update {
                it.copy(
                    isExporting = true,
                    progress = 0f,
                    progressLabel = null
                )
            }
            try {
                val backup =
                    messageBackupRepository.buildBackup(snapshot.selectedThreadIds) { done, total ->
                        _backupUiState.update {
                            it.copy(
                                progress = done.toFloat() / total,
                                progressLabel = application.getString(
                                    R.string.backup_progress_items,
                                    done,
                                    total
                                )
                            )
                        }
                    }
                messageBackupRepository.writeBackupToUri(destination, backup)
                val messageCount = backup.conversations.sumOf { it.messages.size }
                _backupUiState.update {
                    it.copy(
                        isExporting = false,
                        progress = null,
                        progressLabel = null
                    )
                }
                _events.send(
                    MigrationEvent.SmsExportSuccess(
                        application.getString(
                            R.string.backup_success,
                            backup.conversations.size,
                            messageCount
                        )
                    )
                )
            } catch (e: Exception) {
                _backupUiState.update {
                    it.copy(
                        isExporting = false,
                        progress = null,
                        progressLabel = null
                    )
                }
                _events.send(
                    MigrationEvent.SmsExportError(
                        e.message ?: application.getString(R.string.backup_failed)
                    )
                )
            }
        }
    }

    fun toggleContactSelected(contactId: Long, selected: Boolean) {
        _contactsBackupUiState.update { state ->
            val updated = if (selected) {
                state.selectedContactIds + contactId
            } else {
                state.selectedContactIds - contactId
            }
            state.copy(selectedContactIds = updated)
        }
    }

    fun selectAllContacts(contactIds: List<Long>) {
        _contactsBackupUiState.update {
            it.copy(selectedContactIds = contactIds.toSet())
        }
    }

    fun clearContactsSelection() {
        _contactsBackupUiState.update {
            it.copy(selectedContactIds = emptySet())
        }
    }

    fun onContactsFileNameChange(name: String) {
        _contactsBackupUiState.update { it.copy(fileName = name) }
    }

    fun sanitizedContactsFileName(): String {
        return contactsBackupRepository.sanitizeFileName(_contactsBackupUiState.value.fileName)
    }

    fun exportContactsBackup(destination: Uri) {
        if (_contactsBackupUiState.value.isExporting) return

        viewModelScope.launch {
            val snapshot = _contactsBackupUiState.value
            if (snapshot.selectedContactIds.isEmpty()) {
                _events.send(
                    MigrationEvent.ContactsExportError(
                        application.getString(R.string.backup_select_at_least_one_contact)
                    )
                )
                return@launch
            }

            _contactsBackupUiState.update {
                it.copy(
                    isExporting = true,
                    progress = 0f,
                    progressLabel = null
                )
            }
            try {
                val vcf =
                    contactsBackupRepository.buildVcf(snapshot.selectedContactIds) { done, total ->
                        _contactsBackupUiState.update {
                            it.copy(
                                progress = done.toFloat() / total,
                                progressLabel = application.getString(
                                    R.string.backup_progress_items,
                                    done,
                                    total
                                )
                            )
                        }
                    }
                contactsBackupRepository.writeVcfToUri(destination, vcf)
                _contactsBackupUiState.update {
                    it.copy(
                        isExporting = false,
                        progress = null,
                        progressLabel = null
                    )
                }
                _events.send(
                    MigrationEvent.ContactsExportSuccess(
                        application.getString(
                            R.string.backup_contacts_success,
                            snapshot.selectedContactIds.size
                        )
                    )
                )
            } catch (e: Exception) {
                _contactsBackupUiState.update {
                    it.copy(
                        isExporting = false,
                        progress = null,
                        progressLabel = null
                    )
                }
                _events.send(
                    MigrationEvent.ContactsExportError(
                        e.message ?: application.getString(R.string.backup_failed)
                    )
                )
            }
        }
    }

    fun loadSmsImport(uri: Uri) {
        _smsImportSession.value = SmsImportSession(
            fileName = smsImportRepository.readDisplayName(uri)
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backup = smsImportRepository.parseBackup(uri)
                if (backup.conversations.isEmpty()) {
                    _smsImportSession.value = null
                    _events.send(
                        MigrationEvent.SmsImportError(application.getString(R.string.import_empty_file))
                    )
                    return@launch
                }
                _smsImportSession.update { session ->
                    session?.copy(
                        isParsing = false,
                        preview = backup.conversations,
                        selectedThreadIds = backup.conversations.fastMap { c -> c.threadId }.toSet()
                    )
                }
            } catch (e: Exception) {
                _smsImportSession.value = null
                _events.send(
                    MigrationEvent.SmsImportError(
                        e.message ?: application.getString(R.string.import_parse_error)
                    )
                )
            }
        }
    }

    fun clearSmsImport() {
        _smsImportSession.value = null
    }

    fun toggleSmsImportSelected(threadId: Long, selected: Boolean) {
        _smsImportSession.update { session ->
            session?.copy(
                selectedThreadIds = if (selected) {
                    session.selectedThreadIds + threadId
                } else {
                    session.selectedThreadIds - threadId
                }
            )
        }
    }

    fun selectAllSmsImport(threadIds: List<Long>) {
        _smsImportSession.update { session ->
            session?.copy(selectedThreadIds = threadIds.toSet())
        }
    }

    fun clearSmsImportSelection() {
        _smsImportSession.update { session ->
            session?.copy(selectedThreadIds = emptySet())
        }
    }

    fun setSmsImportStrategy(strategy: ImportStrategy) {
        _smsImportSession.update { session ->
            session?.copy(strategy = strategy)
        }
    }

    fun startSmsImport() {
        val session = _smsImportSession.value ?: return
        if (session.isImporting || session.isParsing) return
        viewModelScope.launch(Dispatchers.IO) {
            if (session.selectedThreadIds.isEmpty()) {
                _events.send(
                    MigrationEvent.SmsImportError(application.getString(R.string.backup_select_at_least_one))
                )
                return@launch
            }
            if (!smsImportRepository.isDefaultSmsApp()) {
                _events.send(
                    MigrationEvent.SmsImportError(application.getString(R.string.import_needs_default_sms))
                )
                return@launch
            }
            _smsImportSession.update {
                it?.copy(isImporting = true, progress = 0f, progressLabel = null)
            }
            try {
                val selected = session.preview.orEmpty()
                    .filter { it.threadId in session.selectedThreadIds }
                val result = smsImportRepository.importConversations(
                    selected,
                    session.strategy
                ) { done, total ->
                    _smsImportSession.update {
                        it?.copy(
                            progress = done.toFloat() / total,
                            progressLabel = application.getString(
                                R.string.backup_progress_items,
                                done,
                                total
                            )
                        )
                    }
                }
                _smsImportSession.value = null
                _events.send(
                    MigrationEvent.SmsImportSuccess(
                        application.getString(
                            R.string.import_sms_success,
                            result.imported,
                            result.skipped
                        )
                    )
                )
            } catch (e: Exception) {
                _smsImportSession.update {
                    it?.copy(isImporting = false, progress = null, progressLabel = null)
                }
                _events.send(
                    MigrationEvent.SmsImportError(
                        e.message ?: application.getString(R.string.backup_failed)
                    )
                )
            }
        }
    }

    fun loadContactsImport(uri: Uri) {
        _contactsImportSession.value = ContactsImportSession(
            fileName = contactsImportRepository.readDisplayName(uri)
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = contactsImportRepository.parseVcf(uri)
                if (items.isEmpty()) {
                    _contactsImportSession.value = null
                    _events.send(
                        MigrationEvent.ContactsImportError(application.getString(R.string.import_empty_file))
                    )
                    return@launch
                }
                _contactsImportSession.update { session ->
                    session?.copy(
                        isParsing = false,
                        preview = items,
                        selectedKeys = items.fastMap { it.key }.toSet()
                    )
                }
            } catch (e: Exception) {
                _contactsImportSession.value = null
                _events.send(
                    MigrationEvent.ContactsImportError(
                        e.message ?: application.getString(R.string.import_parse_error)
                    )
                )
            }
        }
    }

    fun clearContactsImport() {
        _contactsImportSession.value = null
    }

    fun toggleContactsImportSelected(key: Int, selected: Boolean) {
        _contactsImportSession.update { session ->
            session?.copy(
                selectedKeys = if (selected) {
                    session.selectedKeys + key
                } else {
                    session.selectedKeys - key
                }
            )
        }
    }

    fun selectAllContactsImport(keys: List<Int>) {
        _contactsImportSession.update { session ->
            session?.copy(selectedKeys = keys.toSet())
        }
    }

    fun clearContactsImportSelection() {
        _contactsImportSession.update { session ->
            session?.copy(selectedKeys = emptySet())
        }
    }

    fun setContactsImportStrategy(strategy: ImportStrategy) {
        _contactsImportSession.update { session ->
            session?.copy(strategy = strategy)
        }
    }

    fun startContactsImport() {
        val session = _contactsImportSession.value ?: return
        if (session.isImporting || session.isParsing) return
        viewModelScope.launch(Dispatchers.IO) {
            if (session.selectedKeys.isEmpty()) {
                _events.send(
                    MigrationEvent.ContactsImportError(
                        application.getString(R.string.backup_select_at_least_one_contact)
                    )
                )
                return@launch
            }
            _contactsImportSession.update {
                it?.copy(isImporting = true, progress = 0f, progressLabel = null)
            }
            try {
                val selected = session.preview.orEmpty()
                    .filter { it.key in session.selectedKeys }
                val result = contactsImportRepository.importContacts(
                    selected,
                    session.strategy
                ) { done, total ->
                    _contactsImportSession.update {
                        it?.copy(
                            progress = done.toFloat() / total,
                            progressLabel = application.getString(
                                R.string.backup_progress_items,
                                done,
                                total
                            )
                        )
                    }
                }
                _contactsImportSession.value = null
                _events.send(
                    MigrationEvent.ContactsImportSuccess(
                        application.getString(
                            R.string.import_contacts_success,
                            result.imported,
                            result.skipped
                        )
                    )
                )
            } catch (e: Exception) {
                _contactsImportSession.update {
                    it?.copy(isImporting = false, progress = null, progressLabel = null)
                }
                _events.send(
                    MigrationEvent.ContactsImportError(
                        e.message ?: application.getString(R.string.backup_failed)
                    )
                )
            }
        }
    }

    fun handleMigrationAction(action: MigrationAction) {
        when (action) {
            is MigrationAction.ImportContacts -> {}

            is MigrationAction.ImportCallLogs -> {}
        }
    }

}

data class SmsImportSession(
    val fileName: String = "",
    val isParsing: Boolean = true,
    val preview: List<BackupConversation>? = null,
    val selectedThreadIds: Set<Long> = emptySet(),
    val strategy: ImportStrategy = ImportStrategy.SKIP_EXISTING,
    val isImporting: Boolean = false,
    val progress: Float? = null,
    val progressLabel: String? = null
)

data class ContactsImportSession(
    val fileName: String = "",
    val isParsing: Boolean = true,
    val preview: List<ImportableContact>? = null,
    val selectedKeys: Set<Int> = emptySet(),
    val strategy: ImportStrategy = ImportStrategy.SKIP_EXISTING,
    val isImporting: Boolean = false,
    val progress: Float? = null,
    val progressLabel: String? = null
)

sealed interface MigrationEvent {
    data class SmsExportSuccess(val message: String) : MigrationEvent
    data class SmsExportError(val message: String) : MigrationEvent
    data class ContactsExportSuccess(val message: String) : MigrationEvent
    data class ContactsExportError(val message: String) : MigrationEvent
    data class SmsImportSuccess(val message: String) : MigrationEvent
    data class SmsImportError(val message: String) : MigrationEvent
    data class ContactsImportSuccess(val message: String) : MigrationEvent
    data class ContactsImportError(val message: String) : MigrationEvent
}

sealed interface MigrationAction {
    data class ImportContacts(
        val source: Pair<String, String>,
        val vCard: Uri
    ) : MigrationAction

    data class ImportCallLogs(
        val json: Uri
    ) : MigrationAction
}
