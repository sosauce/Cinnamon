package com.sosauce.cinnamon.settings.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.settings.BackupUiState

@Composable
fun ContactsBackupDialog(
    contacts: List<CuteContact>,
    uiState: BackupUiState,
    fileNameState: TextFieldState,
    onToggleContact: (contactId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    BackupDialog(
        title = stringResource(R.string.backup_contacts),
        fileExtension = ".vcf",
        entries = contacts,
        uiState = uiState,
        fileNameState = fileNameState,
        hasPermission = PermissionUtils.hasContactsPermission(LocalContext.current),
        missingPermissionText = stringResource(R.string.backup_needs_contacts_permission),
        emptyText = stringResource(R.string.backup_no_contacts),
        itemId = { it.id },
        itemTitle = { it.displayName.ifBlank { it.id.toString() } },
        itemSubtitle = { it.phoneNumbers.firstOrNull()?.number ?: it.accountName },
        onToggle = onToggleContact,
        onSelectAll = onSelectAll,
        onClearSelection = onClearSelection,
        onDismiss = onDismiss,
        onStartBackup = onStartBackup
    )
}
