@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.DefaultGroupChatIcon
import com.sosauce.cinnamon.core.ui.components.toolbars.ToolbarSkeleton
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsState
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.ConversationIcon
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@Composable
fun ConversationTopBar(
    state: ConversationDetailsState,
    onNavigateUp: () -> Unit,
    onHandleCallAction: (CallAction) -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteConversation: () -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
) {

    var showMoreMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val actions = listOf(
        MoreActions(
            onClick = { onNavigate(Screen.ConversationTheming(state.conversation.threadId)) },
            text = R.string.customize,
            icon = R.drawable.palette
        ),
        MoreActions(
            onClick = { showBlockDialog = true },
            text = if (state.conversation.isSoloParticipantBlocked) R.string.unblock else R.string.block,
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
                        if (state.conversation.isSoloParticipantBlocked) R.string.unblock else R.string.block
                    Text(stringResource(text))
                }
            },
            text = {
                val text =
                    if (state.conversation.isSoloParticipantBlocked) R.string.unblock_no_u_sure else R.string.block_are_u_sure
                Text(stringResource(text, state.conversation.participants.first().displayName))
            },
            title = {
                val text = if (state.conversation.isSoloParticipantBlocked) R.string.unblock else R.string.block
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



    ToolbarSkeleton {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onNavigate(Screen.Conversations) },
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
                if (state.conversation.isGroupChat) {
                    DefaultGroupChatIcon()
                } else {
                    ConversationIcon(
                        conversation = state.conversation,
                        size = 40.dp
                    )
                }

            }
            Text(
                text = state.conversation.name,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f),
                overflow = TextOverflow.Ellipsis
            )

            if (!state.conversation.isGroupChat) {
                IconButton(
                    onClick = {
                        val number = state.conversation.participants.first().rawNumber
                        onHandleCallAction(CallAction.LaunchCall(number))
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
                AnimatedDrawable(
                    atEnd = showMoreMenu,
                    drawable = AnimatedDrawableFile.MORE_VERT
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