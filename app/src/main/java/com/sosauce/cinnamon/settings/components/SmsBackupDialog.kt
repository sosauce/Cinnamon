@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.settings.BackupUiState
import com.sosauce.nekobites.components.Spacer

@Composable
fun SmsBackupDialog(
    conversations: List<CuteConversation>,
    uiState: BackupUiState,
    fileNameState: TextFieldState,
    onToggleConversation: (threadId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    BackupDialog(
        title = stringResource(R.string.backup_messages),
        fileExtension = ".json",
        entries = conversations,
        uiState = uiState,
        fileNameState = fileNameState,
        hasPermission = PermissionUtils.hasSmsPermission(LocalContext.current),
        missingPermissionText = stringResource(R.string.backup_needs_sms_permission),
        emptyText = stringResource(R.string.backup_no_conversations),
        itemId = { it.threadId },
        itemTitle = { it.name.ifBlank { it.threadId.toString() } },
        itemSubtitle = { it.snippet },
        onToggle = onToggleConversation,
        onSelectAll = onSelectAll,
        onClearSelection = onClearSelection,
        onDismiss = onDismiss,
        onStartBackup = onStartBackup,
        header = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(10.dp)
                Text(
                    text = stringResource(R.string.backup_mms_note),
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    )
}
