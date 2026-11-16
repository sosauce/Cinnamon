package com.sosauce.cinnamon.settings.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import com.sosauce.cinnamon.settings.BackupUiState

@Composable
fun CallLogsBackupDialog(
    logs: List<CuteCallLog>,
    uiState: BackupUiState,
    fileNameState: TextFieldState,
    onToggleLog: (logId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    BackupDialog(
        title = stringResource(R.string.backup_call_logs),
        fileExtension = ".json",
        entries = logs,
        uiState = uiState,
        fileNameState = fileNameState,
        hasPermission = PermissionUtils.hasCallLogReadPermission(LocalContext.current),
        missingPermissionText = stringResource(R.string.backup_needs_call_log_permission),
        emptyText = stringResource(R.string.backup_no_call_logs),
        itemId = { it.id },
        itemTitle = { it.displayName },
        itemSubtitle = { log ->
            if (log.duration != null) {
                "${log.date} • ${log.duration}"
            } else {
                log.date
            }
        },
        onToggle = onToggleLog,
        onSelectAll = onSelectAll,
        onClearSelection = onClearSelection,
        onDismiss = onDismiss,
        onStartBackup = onStartBackup
    )
}
