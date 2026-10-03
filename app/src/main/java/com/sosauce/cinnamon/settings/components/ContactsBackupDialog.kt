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
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.settings.ContactsBackupUiState

@Composable
fun ContactsBackupDialog(
    contacts: List<CuteContact>,
    uiState: ContactsBackupUiState,
    hasContactsPermission: Boolean,
    onToggleContact: (contactId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isExporting) onDismiss() },
        title = { Text(stringResource(R.string.backup_contacts)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_contacts_desc),
                    style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = stringResource(R.string.backup_vcf_note),
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

                Text(
                    text = stringResource(R.string.backup_select_contacts),
                    style = MaterialTheme.typography.titleSmallEmphasized
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row {
                        TextButton(
                            onClick = onSelectAll,
                            enabled = contacts.isNotEmpty() && !uiState.isExporting,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.select_all))
                        }
                        TextButton(
                            onClick = onClearSelection,
                            enabled = uiState.selectedContactIds.isNotEmpty() && !uiState.isExporting,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.unselect_all))
                        }
                    }
                    Text(
                        text = stringResource(
                            R.string.backup_selected_count,
                            uiState.selectedContactIds.size,
                            contacts.size
                        ),
                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                when {
                    !hasContactsPermission -> {
                        Text(
                            text = stringResource(R.string.backup_needs_contacts_permission),
                            style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    contacts.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.backup_no_contacts),
                            style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    else -> {
                        contacts.fastForEach { contact ->
                            val selected = contact.id in uiState.selectedContactIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        onToggleContact(contact.id, it)
                                    },
                                    enabled = !uiState.isExporting
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = contact.displayName.ifBlank { contact.id.toString() },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMediumEmphasized
                                    )
                                    val subtitle = contact.phoneNumbers.firstOrNull()?.number
                                        ?: contact.accountName
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
                enabled = uiState.selectedContactIds.isNotEmpty() && !uiState.isExporting,
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
