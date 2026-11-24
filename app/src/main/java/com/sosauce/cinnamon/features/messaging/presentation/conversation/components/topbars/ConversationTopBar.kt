@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.DefaultGroupChatIcon
import com.sosauce.cinnamon.core.ui.components.dialogs.BlockUnblockDialog
import com.sosauce.cinnamon.core.ui.components.menus.MoreOptionsMenu
import com.sosauce.cinnamon.core.ui.components.toolbars.ToolbarSkeleton
import com.sosauce.cinnamon.features.messaging.domain.isGroupChat
import com.sosauce.cinnamon.features.messaging.domain.name
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsState
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.MoreOptions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.dialogs.DeleteConversationDialog
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@Composable
fun ConversationTopBar(
    state: ConversationDetailsState,
    onNavigateUp: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteConversation: () -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
) {

    var showMoreMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isSoloParticipantBlocked = state.participants.firstOrNull()?.isBlocked == true
    val name = state.participants.name()
    val actions = listOf(
        MoreOptions(
            onClick = { onHandleConversationActions(ConversationActions.OpenSearch) },
            text = R.string.search,
            icon = R.drawable.search
        ),
        MoreOptions(
            onClick = { onNavigate(Screen.ConversationTheming(state.threadId)) },
            text = R.string.customize,
            icon = R.drawable.palette
        ),
        MoreOptions(
            onClick = { showBlockDialog = true },
            text = if (isSoloParticipantBlocked) R.string.unblock else R.string.block,
            icon = R.drawable.block,
            tint = MaterialTheme.colorScheme.error
        ),
        MoreOptions(
            onClick = { showDeleteDialog = true },
            text = R.string.delete,
            icon = R.drawable.delete,
            tint = MaterialTheme.colorScheme.error
        )
    )

    if (showBlockDialog) {
        val participant = state.participants.firstOrNull()
        if (participant != null) BlockUnblockDialog(
            displayName = participant.displayName,
            isBlocked = isSoloParticipantBlocked,
            onDismissRequest = { showBlockDialog = false },
            onConfirm = {
                onHandleConversationActions(ConversationActions.ToggleBlock)
                showBlockDialog = false
            }
        )
    }
    if (showDeleteDialog) {
        DeleteConversationDialog(
            onDismissRequest = { showDeleteDialog = false },
            onDelete = {
                onDeleteConversation()
                onNavigateUp()
            }
        )
    }



    ToolbarSkeleton(
        onClick = { onNavigate(Screen.AboutConversation(state.threadId)) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledIconButton(
                onClick = { onNavigate(Screen.Conversations) },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = null
                )
            }
            Spacer(Modifier.width(6.dp))
            if (state.participants.isGroupChat()) {
                DefaultGroupChatIcon(size = 40.dp)
            } else {
                DefaultContactIcon(
                    firstLetter = name.firstOrNull(),
                    size = 40.dp,
                    contactPfp = state.participants.firstOrNull()?.photoUri
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                val subtitle = if (state.participants.isGroupChat()) {
                    pluralStringResource(
                        R.plurals.participants_count,
                        state.participants.size,
                        state.participants.size
                    )
                } else null
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            if (!state.participants.isGroupChat()) {
                FilledTonalIconButton(
                    onClick = {
                        state.participants.firstOrNull()?.let {
                            onHandleConversationActions(ConversationActions.CallNumber(it.rawNumber))
                        }
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(
                        IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.call),
                        contentDescription = stringResource(R.string.call)
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
                MoreOptionsMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    options = actions
                )
            }
        }
    }
}
