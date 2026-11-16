package com.sosauce.cinnamon.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
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
import com.sosauce.cinnamon.features.messaging.data.backup.ImportResult
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
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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

/**
 * State of a backup dialog, shared by the messages, contacts and call logs backups
 */
@Stable
data class BackupUiState(
    val selectedIds: Set<Long> = emptySet(),
    val isExporting: Boolean = false,
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

/**
 * State of an import dialog, from the moment a file is picked until it's imported
 *
 * @param T type of the items found in the picked file
 * @param K type of the key identifying an item
 */
data class ImportSession<T, K>(
    val fileName: String = "",
    val isParsing: Boolean = true,
    val preview: List<T>? = null,
    val selectedKeys: Set<K> = emptySet(),
    val strategy: ImportStrategy = ImportStrategy.SKIP_EXISTING,
    val isImporting: Boolean = false,
    val progressDone: Int? = null,
    val progressTotal: Int? = null
)

typealias SmsImportSession = ImportSession<BackupConversation, Long>
typealias ContactsImportSession = ImportSession<ImportableContact, Int>
typealias CallLogsImportSession = ImportSession<ImportableCallLog, Int>

class BackupViewModel(
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
            .asBackupItems()

    val backupContacts: StateFlow<List<CuteContact>> =
        contactsRepository.fetchLatestContacts().asBackupItems()

    val backupCallLogs: StateFlow<List<CuteCallLog>> =
        callLogsRepository.fetchLatestCallLog().asBackupItems()

    val smsBackupFileNameState = TextFieldState(messageBackupRepository.defaultFileName())

    val contactsBackupFileNameState =
        TextFieldState(contactsBackupRepository.defaultFileName())

    val callLogsBackupFileNameState =
        TextFieldState(callLogsBackupRepository.defaultFileName())

    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    private val _contactsBackupUiState = MutableStateFlow(BackupUiState())
    val contactsBackupUiState: StateFlow<BackupUiState> =
        _contactsBackupUiState.asStateFlow()

    private val _callLogsBackupUiState = MutableStateFlow(BackupUiState())
    val callLogsBackupUiState: StateFlow<BackupUiState> =
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

    init {
        selectAllOnFirstLoad(backupConversations, _backupUiState) { it.threadId }
        selectAllOnFirstLoad(backupCallLogs, _callLogsBackupUiState) { it.id }
        selectAllOnFirstLoad(backupContacts, _contactsBackupUiState) { it.id }
    }

    fun toggleConversationSelected(threadId: Long, selected: Boolean) =
        _backupUiState.toggleSelected(threadId, selected)

    fun selectAllConversations(threadIds: List<Long>) = _backupUiState.select(threadIds)

    fun clearConversationSelection() = _backupUiState.select(emptyList())

    fun exportSmsBackup(destination: Uri) = export(
        state = _backupUiState,
        emptySelectionRes = R.string.backup_select_at_least_one
    ) { selectedIds, onProgress ->
        val backup = messageBackupRepository.buildBackup(selectedIds) { done, total ->
            onProgress(done, total)
        }
        messageBackupRepository.writeBackupToUri(destination, backup)
        val messageCount = backup.conversations.sumOf { it.messages.size }
        MigrationEvent.Success(
            R.string.backup_success,
            listOf(backup.conversations.size, messageCount)
        )
    }

    fun toggleContactSelected(contactId: Long, selected: Boolean) =
        _contactsBackupUiState.toggleSelected(contactId, selected)

    fun selectAllContacts(contactIds: List<Long>) = _contactsBackupUiState.select(contactIds)

    fun clearContactsSelection() = _contactsBackupUiState.select(emptyList())

    fun exportContactsBackup(destination: Uri) = export(
        state = _contactsBackupUiState,
        emptySelectionRes = R.string.backup_select_at_least_one_contact
    ) { selectedIds, onProgress ->
        val vcf = contactsBackupRepository.buildVcf(selectedIds) { done, total ->
            onProgress(done, total)
        }
        contactsBackupRepository.writeVcfToUri(destination, vcf)
        MigrationEvent.Success(
            R.string.backup_contacts_success,
            listOf(selectedIds.size)
        )
    }

    fun toggleCallLogSelected(logId: Long, selected: Boolean) =
        _callLogsBackupUiState.toggleSelected(logId, selected)

    fun selectAllCallLogs(logIds: List<Long>) = _callLogsBackupUiState.select(logIds)

    fun clearCallLogsSelection() = _callLogsBackupUiState.select(emptyList())

    fun exportCallLogsBackup(destination: Uri) = export(
        state = _callLogsBackupUiState,
        emptySelectionRes = R.string.backup_select_at_least_one_call_log
    ) { selectedIds, onProgress ->
        val backup = callLogsBackupRepository.buildBackup(selectedIds) { done, total ->
            onProgress(done, total)
        }
        callLogsBackupRepository.writeBackupToUri(destination, backup)
        MigrationEvent.Success(
            R.string.backup_calls_success,
            listOf(backup.calls.size)
        )
    }

    fun loadSmsImport(uri: Uri) = loadImport(
        session = _smsImportSession,
        fileName = messagesImportRepository.readDisplayName(uri),
        key = { it.threadId }
    ) {
        messagesImportRepository.parseBackup(uri).conversations
    }

    fun clearSmsImport() {
        _smsImportSession.value = null
    }

    fun toggleSmsImportSelected(threadId: Long, selected: Boolean) =
        _smsImportSession.toggleKey(threadId, selected)

    fun selectAllSmsImport(threadIds: List<Long>) = _smsImportSession.selectKeys(threadIds)

    fun clearSmsImportSelection() = _smsImportSession.selectKeys(emptyList())

    fun setSmsImportStrategy(strategy: ImportStrategy) = _smsImportSession.setStrategy(strategy)

    fun startSmsImport() = startImport(
        session = _smsImportSession,
        emptySelectionRes = R.string.backup_select_at_least_one,
        successRes = R.string.import_sms_success,
        key = { it.threadId },
        // writing to the SMS provider requires being the default SMS app
        blockingErrorRes = {
            if (messagesImportRepository.isDefaultSmsApp()) null else R.string.import_needs_default_sms
        }
    ) { selected, strategy, onProgress ->
        messagesImportRepository.importConversations(selected, strategy) { done, total ->
            onProgress(done, total)
        }
    }

    fun loadContactsImport(uri: Uri) = loadImport(
        session = _contactsImportSession,
        fileName = contactsImportRepository.readDisplayName(uri),
        key = { it.key }
    ) {
        contactsImportRepository.parseVcf(uri)
    }

    fun clearContactsImport() {
        _contactsImportSession.value = null
    }

    fun toggleContactsImportSelected(key: Int, selected: Boolean) =
        _contactsImportSession.toggleKey(key, selected)

    fun selectAllContactsImport(keys: List<Int>) = _contactsImportSession.selectKeys(keys)

    fun clearContactsImportSelection() = _contactsImportSession.selectKeys(emptyList())

    fun setContactsImportStrategy(strategy: ImportStrategy) =
        _contactsImportSession.setStrategy(strategy)

    fun startContactsImport() = startImport(
        session = _contactsImportSession,
        emptySelectionRes = R.string.backup_select_at_least_one_contact,
        successRes = R.string.import_contacts_success,
        key = { it.key }
    ) { selected, strategy, onProgress ->
        contactsImportRepository.importContacts(selected, strategy) { done, total ->
            onProgress(done, total)
        }
    }

    fun loadCallLogsImport(uri: Uri) = loadImport(
        session = _callLogsImportSession,
        fileName = callLogsImportRepository.readDisplayName(uri),
        key = { it.key }
    ) {
        callLogsImportRepository.parseBackup(uri).mapIndexed { index, log ->
            ImportableCallLog(key = index, log = log)
        }
    }

    fun clearCallLogsImport() {
        _callLogsImportSession.value = null
    }

    fun toggleCallLogsImportSelected(key: Int, selected: Boolean) =
        _callLogsImportSession.toggleKey(key, selected)

    fun selectAllCallLogsImport(keys: List<Int>) = _callLogsImportSession.selectKeys(keys)

    fun clearCallLogsImportSelection() = _callLogsImportSession.selectKeys(emptyList())

    fun setCallLogsImportStrategy(strategy: ImportStrategy) =
        _callLogsImportSession.setStrategy(strategy)

    fun startCallLogsImport() = startImport(
        session = _callLogsImportSession,
        emptySelectionRes = R.string.backup_select_at_least_one_call_log,
        successRes = R.string.import_call_logs_success,
        key = { it.key }
    ) { selected, strategy, onProgress ->
        callLogsImportRepository.importCalls(selected, strategy) { done, total ->
            onProgress(done, total)
        }
    }

    fun handleMigrationAction(action: MigrationAction) {
        when (action) {
            is MigrationAction.ImportContacts -> {}

            is MigrationAction.ImportCallLogs -> {}
        }
    }

    private fun <T> Flow<List<T>>.asBackupItems(): StateFlow<List<T>> =
        catch { emit(emptyList()) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    /**
     * Preselects every item the first time [items] aren't empty
     */
    private fun <T> selectAllOnFirstLoad(
        items: StateFlow<List<T>>,
        state: MutableStateFlow<BackupUiState>,
        id: (T) -> Long
    ) {
        viewModelScope.launch {
            var initialized = false
            items.collect { loaded ->
                if (!initialized && loaded.isNotEmpty()) {
                    initialized = true
                    state.select(loaded.fastMap(id))
                }
            }
        }
    }

    /**
     * Runs [block] with the selected ids while keeping [state] in sync
     * (exporting flag, progress), then reports the outcome.
     */
    private fun export(
        state: MutableStateFlow<BackupUiState>,
        @StringRes emptySelectionRes: Int,
        block: suspend (
            selectedIds: Set<Long>,
            onProgress: (done: Int, total: Int) -> Unit
        ) -> MigrationEvent.Success
    ) {
        if (state.value.isExporting) return

        viewModelScope.launch {
            val snapshot = state.value
            if (snapshot.selectedIds.isEmpty()) {
                _events.send(MigrationEvent.Error(emptySelectionRes))
                return@launch
            }

            state.update {
                it.copy(isExporting = true, progressDone = null, progressTotal = null)
            }
            val event = try {
                block(snapshot.selectedIds) { done, total ->
                    state.update { it.copy(progressDone = done, progressTotal = total) }
                }
            } catch (e: Exception) {
                e.toMigrationError(R.string.backup_failed)
            }
            state.update {
                it.copy(isExporting = false, progressDone = null, progressTotal = null)
            }
            _events.send(event)
        }
    }

    /**
     * Opens an import session for the picked file, then fills it with what [parse] found in it
     */
    private fun <T, K> loadImport(
        session: MutableStateFlow<ImportSession<T, K>?>,
        fileName: String,
        key: (T) -> K,
        parse: suspend () -> List<T>
    ) {
        session.value = ImportSession(fileName = fileName)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = parse()
                if (items.isEmpty()) {
                    session.value = null
                    _events.send(MigrationEvent.Error(R.string.import_empty_file))
                    return@launch
                }
                session.update {
                    it?.copy(
                        isParsing = false,
                        preview = items,
                        selectedKeys = items.map(key).toSet()
                    )
                }
            } catch (e: Exception) {
                session.value = null
                _events.send(e.toMigrationError(R.string.import_parse_error))
            }
        }
    }

    /**
     * Imports the selected items of [session] with [importItems] while keeping its progress in sync,
     * then closes it and reports the outcome.
     *
     * @param blockingErrorRes message of what prevents importing, null when nothing does
     */
    private fun <T, K> startImport(
        session: MutableStateFlow<ImportSession<T, K>?>,
        @StringRes emptySelectionRes: Int,
        @StringRes successRes: Int,
        key: (T) -> K,
        blockingErrorRes: () -> Int? = { null },
        importItems: suspend (
            selected: List<T>,
            strategy: ImportStrategy,
            onProgress: (done: Int, total: Int) -> Unit
        ) -> ImportResult
    ) {
        val snapshot = session.value ?: return
        if (snapshot.isImporting || snapshot.isParsing) return

        viewModelScope.launch(Dispatchers.IO) {
            val errorRes = if (snapshot.selectedKeys.isEmpty()) {
                emptySelectionRes
            } else {
                blockingErrorRes()
            }
            if (errorRes != null) {
                _events.send(MigrationEvent.Error(errorRes))
                return@launch
            }

            session.update {
                it?.copy(isImporting = true, progressDone = null, progressTotal = null)
            }
            try {
                val selected = snapshot.preview.orEmpty()
                    .filter { key(it) in snapshot.selectedKeys }
                val result = importItems(selected, snapshot.strategy) { done, total ->
                    session.update { it?.copy(progressDone = done, progressTotal = total) }
                }
                session.value = null
                _events.send(
                    MigrationEvent.Success(
                        successRes,
                        listOf(result.imported, result.skipped)
                    )
                )
            } catch (e: Exception) {
                session.update {
                    it?.copy(isImporting = false, progressDone = null, progressTotal = null)
                }
                _events.send(e.toMigrationError(R.string.backup_failed))
            }
        }
    }

}

private fun MutableStateFlow<BackupUiState>.toggleSelected(id: Long, selected: Boolean) =
    update { it.copy(selectedIds = it.selectedIds.toggle(id, selected)) }

private fun MutableStateFlow<BackupUiState>.select(ids: List<Long>) =
    update { it.copy(selectedIds = ids.toSet()) }

private fun <T, K> MutableStateFlow<ImportSession<T, K>?>.toggleKey(key: K, selected: Boolean) =
    update { it?.copy(selectedKeys = it.selectedKeys.toggle(key, selected)) }

private fun <T, K> MutableStateFlow<ImportSession<T, K>?>.selectKeys(keys: List<K>) =
    update { it?.copy(selectedKeys = keys.toSet()) }

private fun <T, K> MutableStateFlow<ImportSession<T, K>?>.setStrategy(strategy: ImportStrategy) =
    update { it?.copy(strategy = strategy) }

private fun <E> Set<E>.toggle(element: E, selected: Boolean): Set<E> =
    if (selected) this + element else this - element

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
