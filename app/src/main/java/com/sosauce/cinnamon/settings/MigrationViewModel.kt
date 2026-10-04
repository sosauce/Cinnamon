package com.sosauce.cinnamon.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
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
import com.sosauce.cinnamon.features.messaging.data.backup.MessagesImportRepository
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.features.phone.data.backup.CallLogsBackupRepository
import com.sosauce.cinnamon.features.phone.data.backup.CallLogsImportRepository
import com.sosauce.cinnamon.features.phone.data.backup.ImportableCallLog
import com.sosauce.cinnamon.features.phone.data.repository.CallLogsRepository
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessageBackupUiState(
    val selectedThreadIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

data class ContactsBackupUiState(
    val selectedContactIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

data class CallLogsBackupUiState(
    val selectedLogIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

class MigrationViewModel(
    private val contactsRepository: ContactsRepository,
    private val conversationsRepository: ConversationsRepository,
    private val messageBackupRepository: MessageBackupRepository,
    private val contactsBackupRepository: ContactsBackupRepository,
    private val messagesImportRepository: MessagesImportRepository,
    private val contactsImportRepository: ContactsImportRepository,
    private val callLogsRepository: CallLogsRepository,
    private val callLogsBackupRepository: CallLogsBackupRepository,
    private val callLogsImportRepository: CallLogsImportRepository
) : ViewModel() {

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

    val backupCallLogs: StateFlow<List<CuteCallLog2>> =
        callLogsRepository.fetchLatestCallLog()
            .catch { emit(emptyList()) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val smsBackupFileNameState = TextFieldState(messageBackupRepository.defaultFileName())

    val contactsBackupFileNameState =
        TextFieldState(contactsBackupRepository.defaultFileName())

    val callLogsBackupFileNameState =
        TextFieldState(callLogsBackupRepository.defaultFileName())

    private val _backupUiState = MutableStateFlow(MessageBackupUiState())
    val backupUiState: StateFlow<MessageBackupUiState> = _backupUiState.asStateFlow()

    private val _contactsBackupUiState = MutableStateFlow(ContactsBackupUiState())
    val contactsBackupUiState: StateFlow<ContactsBackupUiState> =
        _contactsBackupUiState.asStateFlow()

    private val _callLogsBackupUiState = MutableStateFlow(CallLogsBackupUiState())
    val callLogsBackupUiState: StateFlow<CallLogsBackupUiState> =
        _callLogsBackupUiState.asStateFlow()

    private val _smsImportSession = MutableStateFlow<SmsImportSession?>(null)
    val smsImportSession: StateFlow<SmsImportSession?> = _smsImportSession.asStateFlow()

    private val _callLogsImportSession = MutableStateFlow<CallLogsImportSession?>(null)
    val callLogsImportSession: StateFlow<CallLogsImportSession?> =
        _callLogsImportSession.asStateFlow()

    private val _contactsImportSession = MutableStateFlow<ContactsImportSession?>(null)
    val contactsImportSession: StateFlow<ContactsImportSession?> =
        _contactsImportSession.asStateFlow()

    private val _events = Channel<MigrationEvent>()
    val events = _events.receiveAsFlow()

    private var smsSelectionInitialized = false
    private var contactsSelectionInitialized = false
    private var callLogsSelectionInitialized = false

    init {
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
            backupCallLogs.collect { logs ->
                if (!callLogsSelectionInitialized && logs.isNotEmpty()) {
                    callLogsSelectionInitialized = true
                    _callLogsBackupUiState.update {
                        it.copy(selectedLogIds = logs.fastMap { c -> c.id }.toSet())
                    }
                }
            }
        }
        viewModelScope.launch {
            backupContacts.collectLatest { contacts ->
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

    fun exportSmsBackup(destination: Uri) {
        if (_backupUiState.value.isExporting) return

        viewModelScope.launch {
            val snapshot = _backupUiState.value
            if (snapshot.selectedThreadIds.isEmpty()) {
                _events.send(
                    MigrationEvent.Error(R.string.backup_select_at_least_one)
                )
                return@launch
            }

            _backupUiState.update {
                it.copy(
                    isExporting = true,
                    progressDone = null,
                    progressTotal = null
                )
            }
            try {
                val backup =
                    messageBackupRepository.buildBackup(snapshot.selectedThreadIds) { done, total ->
                        _backupUiState.update {
                            it.copy(
                                progressDone = done, progressTotal = total
                            )
                        }
                    }
                messageBackupRepository.writeBackupToUri(destination, backup)
                val messageCount = backup.conversations.sumOf { it.messages.size }
                _backupUiState.update {
                    it.copy(
                        isExporting = false,
                        progressDone = null,
                        progressTotal = null
                    )
                }
                _events.send(
                    MigrationEvent.Success(
                        R.string.backup_success,
                        listOf(backup.conversations.size, messageCount)
                    )
                )
            } catch (e: Exception) {
                _backupUiState.update {
                    it.copy(
                        isExporting = false,
                        progressDone = null,
                        progressTotal = null
                    )
                }
                _events.send(
                    e.toMigrationError(R.string.backup_failed)
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

    fun exportContactsBackup(destination: Uri) {
        if (_contactsBackupUiState.value.isExporting) return

        viewModelScope.launch {
            val snapshot = _contactsBackupUiState.value
            if (snapshot.selectedContactIds.isEmpty()) {
                _events.send(
                    MigrationEvent.Error(R.string.backup_select_at_least_one_contact)
                )
                return@launch
            }

            _contactsBackupUiState.update {
                it.copy(
                    isExporting = true,
                    progressDone = null,
                    progressTotal = null
                )
            }
            try {
                val vcf =
                    contactsBackupRepository.buildVcf(snapshot.selectedContactIds) { done, total ->
                        _contactsBackupUiState.update {
                            it.copy(
                                progressDone = done, progressTotal = total
                            )
                        }
                    }
                contactsBackupRepository.writeVcfToUri(destination, vcf)
                _contactsBackupUiState.update {
                    it.copy(
                        isExporting = false,
                        progressDone = null,
                        progressTotal = null
                    )
                }
                _events.send(
                    MigrationEvent.Success(
                        R.string.backup_contacts_success,
                        listOf(snapshot.selectedContactIds.size)
                    )
                )
            } catch (e: Exception) {
                _contactsBackupUiState.update {
                    it.copy(
                        isExporting = false,
                        progressDone = null,
                        progressTotal = null
                    )
                }
                _events.send(
                    e.toMigrationError(R.string.backup_failed)
                )
            }
        }
    }

    fun toggleCallLogSelected(logId: Long, selected: Boolean) {
        _callLogsBackupUiState.update { state ->
            val updated = if (selected) {
                state.selectedLogIds + logId
            } else {
                state.selectedLogIds - logId
            }
            state.copy(selectedLogIds = updated)
        }
    }

    fun selectAllCallLogs(logIds: List<Long>) {
        _callLogsBackupUiState.update {
            it.copy(selectedLogIds = logIds.toSet())
        }
    }

    fun clearCallLogsSelection() {
        _callLogsBackupUiState.update {
            it.copy(selectedLogIds = emptySet())
        }
    }

    fun exportCallLogsBackup(destination: Uri) {
        if (_callLogsBackupUiState.value.isExporting) return

        viewModelScope.launch {
            val snapshot = _callLogsBackupUiState.value
            if (snapshot.selectedLogIds.isEmpty()) {
                _events.send(
                    MigrationEvent.Error(R.string.backup_select_at_least_one_call_log)
                )
                return@launch
            }

            _callLogsBackupUiState.update {
                it.copy(
                    isExporting = true,
                    progressDone = null,
                    progressTotal = null
                )
            }
            try {
                val backup =
                    callLogsBackupRepository.buildBackup(snapshot.selectedLogIds) { done, total ->
                        _callLogsBackupUiState.update {
                            it.copy(
                                progressDone = done, progressTotal = total
                            )
                        }
                    }
                callLogsBackupRepository.writeBackupToUri(destination, backup)
                _callLogsBackupUiState.update {
                    it.copy(
                        isExporting = false,
                        progressDone = null,
                        progressTotal = null
                    )
                }
                _events.send(
                    MigrationEvent.Success(
                        R.string.backup_calls_success,
                        listOf(backup.calls.size)
                    )
                )
            } catch (e: Exception) {
                _callLogsBackupUiState.update {
                    it.copy(
                        isExporting = false,
                        progressDone = null,
                        progressTotal = null
                    )
                }
                _events.send(
                    e.toMigrationError(R.string.backup_failed)
                )
            }
        }
    }

    fun loadSmsImport(uri: Uri) {
        _smsImportSession.value = SmsImportSession(
            fileName = messagesImportRepository.readDisplayName(uri)
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backup = messagesImportRepository.parseBackup(uri)
                if (backup.conversations.isEmpty()) {
                    _smsImportSession.update { null }
                    _events.send(
                        MigrationEvent.Error(R.string.import_empty_file)
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
                _smsImportSession.update { null }
                _events.send(
                    e.toMigrationError(R.string.import_parse_error)
                )
            }
        }
    }

    fun clearSmsImport() {
        _smsImportSession.update { null }
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
                    MigrationEvent.Error(R.string.backup_select_at_least_one)
                )
                return@launch
            }
            if (!messagesImportRepository.isDefaultSmsApp()) {
                _events.send(
                    MigrationEvent.Error(R.string.import_needs_default_sms)
                )
                return@launch
            }
            _smsImportSession.update {
                it?.copy(isImporting = true, progressDone = null, progressTotal = null)
            }
            try {
                val selected = session.preview.orEmpty()
                    .filter { it.threadId in session.selectedThreadIds }
                val result = messagesImportRepository.importConversations(
                    selected,
                    session.strategy
                ) { done, total ->
                    _smsImportSession.update {
                        it?.copy(
                            progressDone = done, progressTotal = total
                        )
                    }
                }
                _smsImportSession.update { null }
                _events.send(
                    MigrationEvent.Success(
                        R.string.import_sms_success,
                        listOf(result.imported, result.skipped)
                    )
                )
            } catch (e: Exception) {
                _smsImportSession.update {
                    it?.copy(isImporting = false, progressDone = null, progressTotal = null)
                }
                _events.send(
                    e.toMigrationError(R.string.backup_failed)
                )
            }
        }
    }

    fun loadContactsImport(uri: Uri) {
        _contactsImportSession.update {
            it?.copy(
                fileName = contactsImportRepository.readDisplayName(uri)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = contactsImportRepository.parseVcf(uri)
                if (items.isEmpty()) {
                    _contactsImportSession.update { null }
                    _events.send(
                        MigrationEvent.Error(R.string.import_empty_file)
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
                _contactsImportSession.update { null }
                _events.send(
                    e.toMigrationError(R.string.import_parse_error)
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
                    MigrationEvent.Error(R.string.backup_select_at_least_one_contact)
                )
                return@launch
            }
            _contactsImportSession.update {
                it?.copy(isImporting = true, progressDone = null, progressTotal = null)
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
                            progressDone = done, progressTotal = total
                        )
                    }
                }
                _contactsImportSession.update { null }
                _events.send(
                    MigrationEvent.Success(
                        R.string.import_contacts_success,
                        listOf(result.imported, result.skipped)
                    )
                )
            } catch (e: Exception) {
                _contactsImportSession.update {
                    it?.copy(isImporting = false, progressDone = null, progressTotal = null)
                }
                _events.send(
                    e.toMigrationError(R.string.backup_failed)
                )
            }
        }
    }

    fun loadCallLogsImport(uri: Uri) {
        _callLogsImportSession.value = CallLogsImportSession(
            fileName = callLogsImportRepository.readDisplayName(uri)
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = callLogsImportRepository.parseBackup(uri)
                if (items.isEmpty()) {
                    _callLogsImportSession.update { null }
                    _events.send(
                        MigrationEvent.Error(R.string.import_empty_file)
                    )
                    return@launch
                }
                _callLogsImportSession.update { session ->
                    val preview = items.mapIndexed { index, log ->
                        ImportableCallLog(key = index, log = log)
                    }
                    session?.copy(
                        isParsing = false,
                        preview = preview,
                        selectedKeys = preview.fastMap { it.key }.toSet()
                    )
                }
            } catch (e: Exception) {
                _callLogsImportSession.update { null }
                _events.send(
                    e.toMigrationError(R.string.import_parse_error)
                )
            }
        }
    }

    fun clearCallLogsImport() {
        _callLogsImportSession.value = null
    }

    fun toggleCallLogsImportSelected(key: Int, selected: Boolean) {
        _callLogsImportSession.update { session ->
            session?.copy(
                selectedKeys = if (selected) {
                    session.selectedKeys + key
                } else {
                    session.selectedKeys - key
                }
            )
        }
    }

    fun selectAllCallLogsImport(keys: List<Int>) {
        _callLogsImportSession.update { session ->
            session?.copy(selectedKeys = keys.toSet())
        }
    }

    fun clearCallLogsImportSelection() {
        _callLogsImportSession.update { session ->
            session?.copy(selectedKeys = emptySet())
        }
    }

    fun setCallLogsImportStrategy(strategy: ImportStrategy) {
        _callLogsImportSession.update { session ->
            session?.copy(strategy = strategy)
        }
    }

    fun startCallLogsImport() {
        val session = _callLogsImportSession.value ?: return
        if (session.isImporting || session.isParsing) return
        viewModelScope.launch(Dispatchers.IO) {
            if (session.selectedKeys.isEmpty()) {
                _events.send(
                    MigrationEvent.Error(R.string.backup_select_at_least_one_call_log)
                )
                return@launch
            }
            _callLogsImportSession.update {
                it?.copy(isImporting = true, progressDone = null, progressTotal = null)
            }
            try {
                val selected = session.preview.orEmpty()
                    .filter { it.key in session.selectedKeys }
                val result = callLogsImportRepository.importCalls(
                    selected,
                    session.strategy
                ) { done, total ->
                    _callLogsImportSession.update {
                        it?.copy(
                            progressDone = done, progressTotal = total
                        )
                    }
                }
                _callLogsImportSession.update { null }
                _events.send(
                    MigrationEvent.Success(
                        R.string.import_call_logs_success,
                        listOf(result.imported, result.skipped)
                    )
                )
            } catch (e: Exception) {
                _callLogsImportSession.update {
                    it?.copy(isImporting = false, progressDone = null, progressTotal = null)
                }
                _events.send(
                    e.toMigrationError(R.string.backup_failed)
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
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

data class ContactsImportSession(
    val fileName: String = "",
    val isParsing: Boolean = true,
    val preview: List<ImportableContact>? = null,
    val selectedKeys: Set<Int> = emptySet(),
    val strategy: ImportStrategy = ImportStrategy.SKIP_EXISTING,
    val isImporting: Boolean = false,
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

data class CallLogsImportSession(
    val fileName: String = "",
    val isParsing: Boolean = true,
    val preview: List<ImportableCallLog>? = null,
    val selectedKeys: Set<Int> = emptySet(),
    val strategy: ImportStrategy = ImportStrategy.SKIP_EXISTING,
    val isImporting: Boolean = false,
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

private fun Exception.toMigrationError(@StringRes fallbackRes: Int): MigrationEvent.Error {
    val details = message
    return if (details != null) {
        MigrationEvent.Error(R.string.error_details, listOf(details))
    } else {
        MigrationEvent.Error(fallbackRes)
    }
}

sealed interface MigrationEvent {
    data class Success(
        @StringRes val messageRes: Int,
        val args: List<Any> = emptyList()
    ) : MigrationEvent

    data class Error(
        @StringRes val messageRes: Int,
        val args: List<Any> = emptyList()
    ) : MigrationEvent
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
