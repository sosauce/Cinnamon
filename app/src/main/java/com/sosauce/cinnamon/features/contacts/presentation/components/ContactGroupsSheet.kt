@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.SelectionCheckbox
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.items.CuteListItemDefaults
import com.sosauce.cinnamon.features.contacts.domain.ContactAccount
import com.sosauce.cinnamon.features.contacts.domain.ContactGroup

@Composable
fun ContactGroupsSheet(
    groups: List<ContactGroup>,
    accounts: List<ContactAccount>,
    memberIds: Set<Long>?,
    onDismissRequest: () -> Unit,
    onToggle: (ContactGroup) -> Unit,
    onCreate: (String, ContactAccount) -> Unit,
    onRename: (ContactGroup, String) -> Unit,
    onDelete: (ContactGroup) -> Unit
) {
    var groupToRename by remember { mutableStateOf<ContactGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<ContactGroup?>(null) }
    var newGroupAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    val newGroupTitle = rememberTextFieldState()
    val showAccounts = accounts.size > 1

    groupToRename?.let { group ->
        RenameGroupDialog(
            group = group,
            onDismissRequest = { groupToRename = null },
            onRename = { title ->
                onRename(group, title)
                groupToRename = null
            }
        )
    }

    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.delete_label)) },
            text = { Text(stringResource(R.string.delete_label_u_sure, group.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(group)
                        groupToDelete = null
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { groupToDelete = null },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Text(
            text = stringResource(R.string.labels),
            style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 10.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_labels),
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 15.dp)
                    )
                }
            }
            itemsIndexed(
                items = groups,
                key = { _, group -> group.id }
            ) { index, group ->
                CuteListItem(
                    modifier = Modifier.animateItem(),
                    onClick = memberIds?.let { { onToggle(group) } },
                    shape = CuteListItemDefaults.getItemShape(index, groups.size),
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    leadingContent = {
                        if (memberIds != null) {
                            SelectionCheckbox(
                                visible = true,
                                checked = group.id in memberIds,
                                modifier = Modifier.padding(start = 15.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.label),
                                contentDescription = null,
                                modifier = Modifier.padding(start = 15.dp)
                            )
                        }
                    },
                    trailingContent = {
                        if (!group.isReadOnly) {
                            IconButton(
                                onClick = { groupToRename = group },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.edit_filled),
                                    contentDescription = stringResource(R.string.rename_label)
                                )
                            }
                            IconButton(
                                onClick = { groupToDelete = group },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete_filled),
                                    contentDescription = stringResource(R.string.delete_label),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    if (showAccounts) {
                        Text(
                            text = group.account.displayName(),
                            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        }

        newGroupAccount?.let { account ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                TextField(
                    state = newGroupTitle,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    placeholder = { Text(stringResource(R.string.new_label)) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = {
                        onCreate(newGroupTitle.text.trim().toString(), account)
                        newGroupTitle.clearText()
                    },
                    enabled = newGroupTitle.text.isNotBlank(),
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = stringResource(R.string.create_label)
                    )
                }
            }
            if (showAccounts) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 15.dp)
                        .padding(bottom = 10.dp)
                ) {
                    accounts.forEach { option ->
                        AccountOption(
                            account = option,
                            selected = option == account,
                            onClick = { newGroupAccount = option }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountOption(
    account: ContactAccount,
    selected: Boolean,
    onClick: () -> Unit
) {
    val corners by animateDpAsState(if (selected) 50.dp else 12.dp)
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(corners),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColorFor(color)
        )
    ) {
        AnimatedVisibility(
            visible = selected,
            modifier = Modifier.padding(end = 5.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null
            )
        }
        Text(
            text = account.displayName(),
            style = MaterialTheme.typography.labelLargeEmphasized
        )
    }
}

@Composable
private fun RenameGroupDialog(
    group: ContactGroup,
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit
) {
    val title = rememberTextFieldState(group.title)
    val newTitle = title.text.trim().toString()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.label),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.rename_label)) },
        text = {
            TextField(
                state = title,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(newTitle) },
                enabled = newTitle.isNotEmpty() && newTitle != group.title,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ContactAccount.displayName(): String =
    name ?: stringResource(R.string.device)
