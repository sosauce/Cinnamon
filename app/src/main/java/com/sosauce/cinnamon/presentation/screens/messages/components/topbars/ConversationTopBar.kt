@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.presentation.screens.messages.components.topbars

import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.messages.ConversationDetailsState
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.DefaultGroupChatIcon
import com.sosauce.cinnamon.presentation.shared_components.toolbars.ToolbarSkeleton
import com.sosauce.cinnamon.utils.getContactId
import com.sosauce.cinnamon.utils.getContactPfpUriFromId
import com.sosauce.cinnamon.utils.getItemShape
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@Composable
fun ConversationTopBar(
    modifier: Modifier = Modifier,
    state: ConversationDetailsState,
    onNavigateUp: () -> Unit,
    onHandleCallAction: (CallAction) -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteConversation: () -> Unit
) {

    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isGroupChat = state.recipients.size > 1
    val actions = listOf(
        MoreActions(
            onClick = { onNavigate(Screen.ConversationTheming(state.threadId)) },
            text = R.string.customize,
            icon = R.drawable.palette
        ),
        MoreActions(
            onClick = { showBlockDialog = true },
            text = R.string.block,
            icon = R.drawable.block,
            tint = MaterialTheme.colorScheme.error
        ),
        MoreActions(
            onClick = { showDeleteDialog = true },
            text = R.string.delete,
            icon = R.drawable.delete,
            tint = MaterialTheme.colorScheme.error
        )
    )

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.block),
                    contentDescription = stringResource(R.string.block)
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockDialog = false }
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO -> onBlock()
                        showBlockDialog = false
                    }
                ) { Text(stringResource(R.string.block)) }
            },
            text = { Text(stringResource(R.string.block)) },
            title = { Text(stringResource(R.string.block)) },
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.delete)
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConversation()
                        onNavigateUp()
                    }
                ) { Text(stringResource(R.string.delete)) }
            },
            text = { Text(stringResource(R.string.delete_convo_u_sure)) },
            title = { Text(stringResource(R.string.delete_convo)) },
        )
    }



    ToolbarSkeleton(
        onClick = if (isGroupChat) { null } else {
            {}
//            TODO { onNavigate(Screen.ContactDetails(state.recipients.first())) }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onNavigateUp,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = null
                )
            }
            if (isGroupChat) {
                DefaultGroupChatIcon(
                    modifier = Modifier.padding(end = 10.dp),
                )
            } else {
                DefaultContactIcon(
                    firstLetter = state.recipients.firstOrNull()?.firstOrNull(),
                    modifier = Modifier.padding(end = 10.dp),
                    size = 38.dp,
                    contactPfp = state.recipients.firstOrNull()?.getContactId(context)?.getContactPfpUriFromId() ?: Uri.EMPTY
                )
            }
            Text(
                text = buildString {
                    state.nameOrBeautifiedRecipients.fastForEachIndexed { index, recipient ->
                        append(recipient)
                        if (index != state.nameOrBeautifiedRecipients.lastIndex) {
                            append(", ")
                        }
                    }
                },
                maxLines = 1,
                modifier = Modifier
                    .weight(1f),
                overflow = TextOverflow.Ellipsis
            )

            if (!isGroupChat) {
                IconButton(
                    onClick = {
                        onHandleCallAction(CallAction.LaunchCall(state.recipients.first()))
                    },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.call),
                        contentDescription = null
                    )
                }
            }
            IconButton(
                onClick = { showMoreMenu = true },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null
                )
                DropdownMenuPopup(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    DropdownMenuGroup(
                        shapes = MenuDefaults.groupShapes()
                    ) {
                        actions.fastForEachIndexed { index, action ->
                            DropdownMenuItem(
                                onClick = action.onClick,
                                shape = MenuDefaults.getItemShape(index, actions.lastIndex),
                                text = {
                                    Text(
                                        text = stringResource(action.text),
                                        color = action.tint ?: LocalContentColor.current
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(action.icon),
                                        contentDescription = null,
                                        tint = action.tint ?: LocalContentColor.current
                                    )
                                }
                            )
                        }
                    }
                }
            }

        }
    }
}

data class MoreActions(
    val onClick: () -> Unit,
    val text: Int,
    val icon: Int,
    val tint: Color? = null
)