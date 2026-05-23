@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.presentation.screens.messages.components.topbars

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import com.sosauce.cinnamon.presentation.screens.messages.ConversationActions
import com.sosauce.cinnamon.presentation.screens.messages.ConversationDetailsState
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.DefaultGroupChatIcon
import com.sosauce.cinnamon.presentation.shared_components.animations.AnimatedMoreIcon
import com.sosauce.cinnamon.presentation.shared_components.toolbars.ToolbarSkeleton
import com.sosauce.cinnamon.utils.getItemShape
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@Composable
fun SharedTransitionScope.ConversationTopBar(
    state: ConversationDetailsState,
    onNavigateUp: () -> Unit,
    onHandleCallAction: (CallAction) -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteConversation: () -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
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
            text = if (state.isSoloRecipientBlocked) R.string.unblock else R.string.block,
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
                    contentDescription = null
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHandleConversationActions(ConversationActions.ToggleBlock)
                        showBlockDialog = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    val text =
                        if (state.isSoloRecipientBlocked) R.string.unblock else R.string.block
                    Text(stringResource(text))
                }
            },
            text = {
                val text =
                    if (state.isSoloRecipientBlocked) R.string.unblock_no_u_sure else R.string.block_are_u_sure
                Text(stringResource(text, state.recipients.first()))
            },
            title = {
                val text = if (state.isSoloRecipientBlocked) R.string.unblock else R.string.block
                Text(stringResource(text))
            }
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
        onClick = { onNavigate(Screen.AboutConversation(state.threadId)) }
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
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
            ) {
                if (isGroupChat) {
                    DefaultGroupChatIcon()
                } else {
                    DefaultContactIcon(
                        firstLetter = state.nameOrBeautifiedRecipients.firstOrNull()?.firstOrNull(),
                        size = 38.dp,
                        contactPhoneNumber = state.recipients.firstOrNull()
                    )
                }

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
                AnimatedMoreIcon(showMoreMenu)
                DropdownMenuPopup(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    DropdownMenuGroup(
                        shapes = MenuDefaults.groupShapes()
                    ) {
                        actions.fastForEachIndexed { index, action ->
                            DropdownMenuItem(
                                onClick = {
                                    action.onClick()
                                    showMoreMenu = false
                                },
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