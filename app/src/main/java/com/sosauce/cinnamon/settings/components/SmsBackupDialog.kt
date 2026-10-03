@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.settings.MessageBackupUiState

@Composable
fun SmsBackupDialog(
    conversations: List<CuteConversation>,
    uiState: MessageBackupUiState,
    hasSmsPermission: Boolean,
    onToggleConversation: (threadId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isExporting) onDismiss() },
        title = { Text(stringResource(R.string.backup_messages)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_messages_desc),
                    style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = stringResource(R.string.backup_mms_note),
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = uiState.fileName,
                    onValueChange = onFileNameChange,
                    label = { Text(stringResource(R.string.backup_file_name)) },
                    singleLine = true,
                    enabled = !uiState.isExporting,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row {
                        TextButton(
                            onClick = onSelectAll,
                            enabled = conversations.isNotEmpty() && !uiState.isExporting,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.select_all))
                        }
                        TextButton(
                            onClick = onClearSelection,
                            enabled = uiState.selectedThreadIds.isNotEmpty() && !uiState.isExporting,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.unselect_all))
                        }
                    }
                    Text(
                        text = "${uiState.selectedThreadIds.size}/${conversations.size}",
                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                when {
                    !hasSmsPermission -> {
                        Text(
                            text = stringResource(R.string.backup_needs_sms_permission),
                            style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    conversations.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.backup_no_conversations),
                            style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    else -> {
                        conversations.fastForEach { conversation ->
                            val selected = conversation.threadId in uiState.selectedThreadIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        onToggleConversation(conversation.threadId, it)
                                    },
                                    enabled = !uiState.isExporting
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = conversation.name.ifBlank { conversation.threadId.toString() },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMediumEmphasized
                                    )
                                    if (conversation.snippet.isNotBlank()) {
                                        Text(
                                            text = conversation.snippet,
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
                }

                if (uiState.isExporting) {
                    BackupProgressIndicator(
                        progress = uiState.progress,
                        label = uiState.progressLabel
                    )
                }

            }
        },
        confirmButton = {
            TextButton(
                onClick = onStartBackup,
                enabled = uiState.selectedThreadIds.isNotEmpty() && !uiState.isExporting,
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
