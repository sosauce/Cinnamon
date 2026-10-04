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
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.settings.ContactsBackupUiState

@Composable
fun ContactsBackupDialog(
    contacts: List<CuteContact>,
    uiState: ContactsBackupUiState,
    fileNameState: TextFieldState,
    hasContactsPermission: Boolean,
    onToggleContact: (contactId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStartBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isExporting) onDismiss() },
        title = { Text(stringResource(R.string.backup_contacts)) },
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
                    OutlinedTextField(
                        state = fileNameState,
                        label = {
                            Text(
                                text = stringResource(R.string.backup_file_name, ".vcf")
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
                            text = "${uiState.selectedContactIds.size}/${contacts.size}",
                            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                when {
                    !hasContactsPermission -> {
                        item {
                            Text(
                                text = stringResource(R.string.backup_needs_contacts_permission),
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }

                    contacts.isEmpty() -> {
                        item {
                            Text(
                                text = stringResource(R.string.backup_no_contacts),
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    else -> {
                        items(
                            items = contacts,
                            key = { it.id }
                        ) { contact ->
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
