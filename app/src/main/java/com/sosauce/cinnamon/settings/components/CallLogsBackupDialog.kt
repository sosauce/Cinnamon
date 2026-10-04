@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
import com.sosauce.cinnamon.settings.CallLogsBackupUiState

@Composable
fun CallLogsBackupDialog(
    logs: List<CuteCallLog2>,
    uiState: CallLogsBackupUiState,
    hasCallLogPermission: Boolean,
    onToggleLog: (logId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isExporting) onDismiss() },
        title = { Text(stringResource(R.string.backup_call_logs)) },
        icon = {
            Icon(
                painter = painterResource(R.drawable.phone),
                contentDescription = null
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.backup_call_logs_desc),
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.fileName,
                        onValueChange = onFileNameChange,
                        label = {
                            Text(
                                text = stringResource(R.string.backup_file_name, ".json")
                            )
                        },
                        singleLine = true,
                        enabled = !uiState.isExporting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row {
                            TextButton(
                                onClick = onSelectAll,
                                enabled = logs.isNotEmpty() && !uiState.isExporting,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(stringResource(R.string.select_all))
                            }
                            TextButton(
                                onClick = onClearSelection,
                                enabled = uiState.selectedLogIds.isNotEmpty() && !uiState.isExporting,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(stringResource(R.string.unselect_all))
                            }
                        }
                        Text(
                            text = "${uiState.selectedLogIds.size}/${logs.size}",
                            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                when {
                    !hasCallLogPermission -> {
                        item {
                            Text(
                                text = stringResource(R.string.backup_needs_call_log_permission),
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }

                    logs.isEmpty() -> {
                        item {
                            Text(
                                text = stringResource(R.string.backup_no_call_logs),
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    else -> {
                        items(
                            items = logs,
                            key = { it.id }
                        ) { log ->
                            val selected = log.id in uiState.selectedLogIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        onToggleLog(log.id, it)
                                    },
                                    enabled = !uiState.isExporting
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = log.displayName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMediumEmphasized
                                    )
                                    Text(
                                        text = if (log.duration != null) {
                                            "${log.date} • ${log.duration}"
                                        } else {
                                            log.date
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.isExporting) {
                    item {
                        BackupProgressIndicator(
                            done = uiState.progressDone,
                            total = uiState.progressTotal
                        )
                    }
                }

            }
        },
        confirmButton = {
            TextButton(
                onClick = onStartBackup,
                enabled = uiState.selectedLogIds.isNotEmpty() && !uiState.isExporting,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.backup_start))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isExporting,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
