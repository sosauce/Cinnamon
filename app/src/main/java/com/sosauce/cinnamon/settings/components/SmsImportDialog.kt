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
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import com.sosauce.cinnamon.settings.SmsImportSession

@Composable
fun SmsImportDialog(
    session: SmsImportSession,
    onToggleConversation: (threadId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStrategyChange: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit,
    onStartImport: () -> Unit
) {
    val busy = session.isParsing || session.isImporting
    val preview = session.preview

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.import_sms)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = session.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (preview == null) {
                    BackupProgressIndicator(
                        progress = null,
                        label = stringResource(R.string.import_parsing)
                    )
                } else {
                    val totalMessages = preview.sumOf { it.messages.size }
                    Text(
                        text = stringResource(
                            R.string.import_preview_sms,
                            preview.size,
                            totalMessages
                        ),
                        style = MaterialTheme.typography.bodyMediumEmphasized
                    )

                    ImportStrategySelector(
                        strategy = session.strategy,
                        enabled = !session.isImporting,
                        onStrategyChange = onStrategyChange
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row {
                            TextButton(
                                onClick = onSelectAll,
                                enabled = !session.isImporting,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(stringResource(R.string.select_all))
                            }
                            TextButton(
                                onClick = onClearSelection,
                                enabled = session.selectedThreadIds.isNotEmpty() && !session.isImporting,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(stringResource(R.string.unselect_all))
                            }
                        }
                        Text(
                            text = stringResource(
                                R.string.backup_selected_count,
                                session.selectedThreadIds.size,
                                preview.size
                            ),
                            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    preview.fastForEach { conversation ->
                        val selected = conversation.threadId in session.selectedThreadIds
                        val name = conversation.participants.joinToString(", ") { it.displayName }
                            .ifBlank { conversation.threadId.toString() }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = {
                                    onToggleConversation(conversation.threadId, it)
                                },
                                enabled = !session.isImporting
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMediumEmphasized
                                )
                                Text(
                                    text = stringResource(
                                        R.string.import_messages_count,
                                        conversation.messages.size
                                    ),
                                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    if (session.isImporting) {
                        BackupProgressIndicator(
                            progress = session.progress,
                            label = session.progressLabel
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onStartImport,
                enabled = preview != null &&
                        session.selectedThreadIds.isNotEmpty() &&
                        !session.isImporting &&
                        !session.isParsing,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.import_start))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
