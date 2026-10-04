@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
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
import com.sosauce.cinnamon.core.ui.ValidFileNameTransformation
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.settings.MessageBackupUiState
import com.sosauce.nekobites.components.Spacer

@Composable
fun SmsBackupDialog(
    conversations: List<CuteConversation>,
    uiState: MessageBackupUiState,
    fileNameState: TextFieldState,
    hasSmsPermission: Boolean,
    onToggleConversation: (threadId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isExporting) onDismiss() },
        title = { Text(stringResource(R.string.backup_messages)) },
        icon = {
            Icon(
                painter = painterResource(R.drawable.migrate),
                contentDescription = null
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
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

                item {
                    OutlinedTextField(
                        state = fileNameState,
                        label = {
                            Text(
                                text = stringResource(R.string.backup_file_name, ".json")
                            )
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        inputTransformation = ValidFileNameTransformation,
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
                }

                when {
                    !hasSmsPermission -> {
                        item {
                            Text(
                                text = stringResource(R.string.backup_needs_sms_permission),
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }

                    conversations.isEmpty() -> {
                        item {
                            Text(
                                text = stringResource(R.string.backup_no_conversations),
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    else -> {
                        items(
                            items = conversations,
                            key = { it.threadId }
                        ) { conversation ->
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
