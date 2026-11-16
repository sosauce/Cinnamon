@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.ValidFileNameTransformation
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import com.sosauce.cinnamon.settings.BackupUiState
import com.sosauce.cinnamon.settings.ImportSession

/**
 * Shared backup progress UI. When unknown (e.g. right before the
 * first item completes) an indeterminate wavy indicator is shown instead.
 */
@Composable
fun BackupProgressIndicator(
    done: Int?,
    total: Int?,
    modifier: Modifier = Modifier,
    labelOverride: String? = null
) {
    val progress = if (done != null && total != null && total > 0) {
        (done.toFloat() / total).coerceIn(0f, 1f)
    } else {
        null
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = labelOverride ?: if (done != null && total != null) {
                stringResource(R.string.backup_progress_items, done, total)
            } else {
                stringResource(R.string.backup_exporting_generic)
            },
            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun ImportStrategySelector(
    strategy: ImportStrategy,
    enabled: Boolean,
    onStrategyChange: (ImportStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.import_strategy_title),
            style = MaterialTheme.typography.titleSmallEmphasized
        )
        ImportStrategy.entries.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = strategy == option,
                    onClick = { onStrategyChange(option) },
                    enabled = enabled
                )
                Text(
                    text = stringResource(
                        when (option) {
                            ImportStrategy.SKIP_EXISTING -> R.string.import_skip_existing
                            ImportStrategy.IMPORT_ALL -> R.string.import_import_all
                        }
                    ),
                    style = MaterialTheme.typography.bodyMediumEmphasized
                )
            }
        }
    }
}

/**
 * Dialog shared by every backup: file name, selectable list of [entries] and export progress.
 *
 * @param header optional content shown above the file name
 */
@Composable
fun <T> BackupDialog(
    title: String,
    fileExtension: String,
    entries: List<T>,
    uiState: BackupUiState,
    fileNameState: TextFieldState,
    hasPermission: Boolean,
    missingPermissionText: String,
    emptyText: String,
    itemId: (T) -> Long,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    onToggle: (id: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit,
    header: (@Composable () -> Unit)? = null
) {
    val isExporting = uiState.isExporting

    MigrationDialog(
        title = title,
        icon = R.drawable.migrate,
        confirmText = stringResource(R.string.backup_start),
        confirmEnabled = uiState.selectedIds.isNotEmpty() && !isExporting,
        busy = isExporting,
        onConfirm = onStartBackup,
        onDismiss = onDismiss
    ) {
        if (header != null) {
            item { header() }
        }

        item {
            TextField(
                state = fileNameState,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                label = {
                    Text(
                        text = stringResource(R.string.backup_file_name, fileExtension)
                    )
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                inputTransformation = ValidFileNameTransformation,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            SelectionHeader(
                selectedCount = uiState.selectedIds.size,
                totalCount = entries.size,
                selectAllEnabled = entries.isNotEmpty() && !isExporting,
                clearEnabled = uiState.selectedIds.isNotEmpty() && !isExporting,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection
            )
        }

        when {
            !hasPermission -> {
                item {
                    Text(
                        text = missingPermissionText,
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }

            entries.isEmpty() -> {
                item {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            else -> {
                items(
                    items = entries,
                    key = { itemId(it) }
                ) { entry ->
                    val id = itemId(entry)
                    SelectableRow(
                        checked = id in uiState.selectedIds,
                        enabled = !isExporting,
                        onCheckedChange = { onToggle(id, it) },
                        title = itemTitle(entry),
                        subtitle = itemSubtitle(entry)
                    )
                }
            }
        }

        if (isExporting) {
            item {
                BackupProgressIndicator(
                    done = uiState.progressDone,
                    total = uiState.progressTotal
                )
            }
        }
    }
}

/**
 * Dialog shared by every import: picked file name, then once parsed a selectable preview
 * of what it contains, the import strategy and the import progress.
 */
@Composable
fun <T, K : Any> ImportDialog(
    title: String,
    @DrawableRes icon: Int,
    session: ImportSession<T, K>,
    previewSummary: @Composable (preview: List<T>) -> String,
    itemKey: (T) -> K,
    itemTitle: @Composable (T) -> String,
    itemSubtitle: @Composable (T) -> String,
    onToggle: (key: K, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStrategyChange: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit,
    onStartImport: () -> Unit
) {
    val busy = session.isParsing || session.isImporting
    val preview = session.preview

    MigrationDialog(
        title = title,
        icon = icon,
        confirmText = stringResource(R.string.import_start),
        confirmEnabled = preview != null && session.selectedKeys.isNotEmpty() && !busy,
        busy = busy,
        onConfirm = onStartImport,
        onDismiss = onDismiss
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
                    text = previewSummary(preview),
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
                SelectionHeader(
                    selectedCount = session.selectedKeys.size,
                    totalCount = preview.size,
                    selectAllEnabled = !session.isImporting,
                    clearEnabled = session.selectedKeys.isNotEmpty() && !session.isImporting,
                    onSelectAll = onSelectAll,
                    onClearSelection = onClearSelection
                )
            }

            items(
                items = preview,
                key = { itemKey(it) }
            ) { entry ->
                val key = itemKey(entry)
                SelectableRow(
                    checked = key in session.selectedKeys,
                    enabled = !session.isImporting,
                    onCheckedChange = { onToggle(key, it) },
                    title = itemTitle(entry),
                    subtitle = itemSubtitle(entry)
                )
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
}

/**
 * Frame of the backup and import dialogs, can't be dismissed while [busy]
 */
@Composable
private fun MigrationDialog(
    title: String,
    @DrawableRes icon: Int,
    confirmText: String,
    confirmEnabled: Boolean,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(confirmText)
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

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    totalCount: Int,
    selectAllEnabled: Boolean,
    clearEnabled: Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            TextButton(
                onClick = onSelectAll,
                enabled = selectAllEnabled,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.select_all))
            }
            TextButton(
                onClick = onClearSelection,
                enabled = clearEnabled,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.unselect_all))
            }
        }
        Text(
            text = "$selectedCount/$totalCount",
            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

/**
 * Checkbox row with a title and, when not blank, a subtitle
 */
@Composable
private fun SelectableRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMediumEmphasized
            )
            if (subtitle.isNotBlank()) {
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
}
