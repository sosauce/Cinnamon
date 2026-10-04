package com.sosauce.cinnamon.settings.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import com.sosauce.cinnamon.settings.CallLogsImportSession

@Composable
fun CallLogsImportDialog(
    session: CallLogsImportSession,
    onToggleLog: (key: Int, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStrategyChange: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit,
    onStartImport: () -> Unit
) {
    val busy = session.isParsing || session.isImporting
    val preview = session.preview
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.import_call_logs)) },
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
                        text = session.fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (preview == null) {
                    item {
                        BackupProgressIndicator(
                            done = null,
                            total = null,
                            labelOverride = stringResource(R.string.import_parsing)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(
                                R.string.import_preview_calls,
                                preview.size
                            ),
                            style = MaterialTheme.typography.bodyMediumEmphasized
                        )
                    }

                    item {
                        ImportStrategySelector(
                            strategy = session.strategy,
                            enabled = !session.isImporting,
                            onStrategyChange = onStrategyChange
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
                                    enabled = !session.isImporting,
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(stringResource(R.string.select_all))
                                }
                                TextButton(
                                    onClick = onClearSelection,
                                    enabled = session.selectedKeys.isNotEmpty() && !session.isImporting,
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(stringResource(R.string.unselect_all))
                                }
                            }
                            Text(
                                text = "${session.selectedKeys.size}/${preview.size}",
                                style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    items(
                        items = preview,
                        key = { it.key }
                    ) { item ->
                        val selected = item.key in session.selectedKeys
                        val log = item.log
                        val title = log.cachedName?.takeIf { it.isNotBlank() }
                            ?: log.number.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.private_number)
                        val subtitle = remember(log.dateMillis, log.durationSeconds) {
                            val date = DateUtils.formatDateTime(
                                context,
                                log.dateMillis,
                                DateUtils.FORMAT_ABBREV_MONTH
                            )
                            if (log.durationSeconds > 0) {
                                "$date • ${DateUtils.formatElapsedTime(log.durationSeconds)}"
                            } else {
                                date
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { onToggleLog(item.key, it) },
                                enabled = !session.isImporting
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMediumEmphasized
                                )
                                Text(
                                    text = subtitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    if (session.isImporting) {
                        item {
                            BackupProgressIndicator(
                                done = session.progressDone,
                                total = session.progressTotal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onStartImport,
                enabled = preview != null &&
                        session.selectedKeys.isNotEmpty() &&
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
