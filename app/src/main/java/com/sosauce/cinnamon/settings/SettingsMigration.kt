package com.sosauce.cinnamon.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.settings.components.ContactsBackupDialog
import com.sosauce.cinnamon.settings.components.ContactsImportDialog
import com.sosauce.cinnamon.settings.components.PlainSettingsCard
import com.sosauce.cinnamon.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.settings.components.SmsBackupDialog
import com.sosauce.cinnamon.settings.components.SmsImportDialog
import com.sosauce.nekobites.helpers.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsMigration() {
    val context = LocalContext.current
    val viewModel = koinViewModel<MigrationViewModel>()

    val conversations by viewModel.backupConversations.collectAsStateWithLifecycle()
    val smsState by viewModel.backupUiState.collectAsStateWithLifecycle()
    val contacts by viewModel.backupContacts.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsBackupUiState.collectAsStateWithLifecycle()
    val smsImport by viewModel.smsImportSession.collectAsStateWithLifecycle()
    val contactsImport by viewModel.contactsImportSession.collectAsStateWithLifecycle()

    var showSmsDialog by remember { mutableStateOf(false) }
    var showContactsDialog by remember { mutableStateOf(false) }

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED
    val hasContactsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    ObserveAsEvents(viewModel.events) { event ->
        val message = when (event) {
            is MigrationEvent.SmsExportSuccess -> event.message
            is MigrationEvent.SmsExportError -> event.message
            is MigrationEvent.ContactsExportSuccess -> event.message
            is MigrationEvent.ContactsExportError -> event.message
            is MigrationEvent.SmsImportSuccess -> event.message
            is MigrationEvent.SmsImportError -> event.message
            is MigrationEvent.ContactsImportSuccess -> event.message
            is MigrationEvent.ContactsImportError -> event.message
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val smsDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                viewModel.exportSmsBackup(uri)
            }
        }
    val contactsDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/vcard")) { uri ->
            if (uri != null) {
                viewModel.exportContactsBackup(uri)
            }
        }
    val smsImportPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.loadSmsImport(uri)
            }
        }
    val contactsImportPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.loadContactsImport(uri)
            }
        }

    Column {
        SettingsWithTitle(title = R.string.migration) {
            PlainSettingsCard(
                onClick = { showSmsDialog = true },
                topDp = 24.dp,
                bottomDp = 2.dp,
                text = stringResource(R.string.backup_messages),
                optionalDescription = R.string.backup_messages_desc
            )
            PlainSettingsCard(
                onClick = { showContactsDialog = true },
                topDp = 2.dp,
                bottomDp = 2.dp,
                text = stringResource(R.string.backup_contacts),
                optionalDescription = R.string.backup_contacts_desc
            )
            PlainSettingsCard(
                onClick = { smsImportPicker.launch(arrayOf("application/json", "text/plain")) },
                topDp = 2.dp,
                bottomDp = 2.dp,
                text = stringResource(R.string.import_sms),
                optionalDescription = R.string.import_sms_desc
            )
            PlainSettingsCard(
                onClick = { contactsImportPicker.launch(arrayOf("text/vcard", "text/x-vcard")) },
                topDp = 2.dp,
                bottomDp = 24.dp,
                text = stringResource(R.string.import_contacts),
                optionalDescription = R.string.import_contacts_desc
            )
        }
    }

    if (showSmsDialog) {
        SmsBackupDialog(
            conversations = conversations,
            uiState = smsState,
            hasSmsPermission = hasSmsPermission,
            onToggleConversation = viewModel::toggleConversationSelected,
            onSelectAll = { viewModel.selectAllConversations(conversations.map { it.threadId }) },
            onClearSelection = viewModel::clearConversationSelection,
            onFileNameChange = viewModel::onBackupFileNameChange,
            onDismiss = { showSmsDialog = false },
            onStartBackup = {
                smsDocumentLauncher.launch(viewModel.sanitizedSmsFileName())
            }
        )
    }

    if (showContactsDialog) {
        ContactsBackupDialog(
            contacts = contacts,
            uiState = contactsState,
            hasContactsPermission = hasContactsPermission,
            onToggleContact = viewModel::toggleContactSelected,
            onSelectAll = { viewModel.selectAllContacts(contacts.map { it.id }) },
            onClearSelection = viewModel::clearContactsSelection,
            onFileNameChange = viewModel::onContactsFileNameChange,
            onDismiss = { showContactsDialog = false },
            onStartBackup = {
                contactsDocumentLauncher.launch(viewModel.sanitizedContactsFileName())
            }
        )
    }

    smsImport?.let { session ->
        SmsImportDialog(
            session = session,
            onToggleConversation = viewModel::toggleSmsImportSelected,
            onSelectAll = {
                viewModel.selectAllSmsImport(session.preview.orEmpty().map { it.threadId })
            },
            onClearSelection = viewModel::clearSmsImportSelection,
            onStrategyChange = viewModel::setSmsImportStrategy,
            onDismiss = viewModel::clearSmsImport,
            onStartImport = viewModel::startSmsImport
        )
    }

    contactsImport?.let { session ->
        ContactsImportDialog(
            session = session,
            onToggleContact = viewModel::toggleContactsImportSelected,
            onSelectAll = {
                viewModel.selectAllContactsImport(session.preview.orEmpty().map { it.key })
            },
            onClearSelection = viewModel::clearContactsImportSelection,
            onStrategyChange = viewModel::setContactsImportStrategy,
            onDismiss = viewModel::clearContactsImport,
            onStartImport = viewModel::startContactsImport
        )
    }
}
