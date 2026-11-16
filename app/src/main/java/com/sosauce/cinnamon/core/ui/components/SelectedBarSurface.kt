@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.utils.LocalScreen
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.dialogs.DeleteConversationsDialog
import com.sosauce.nekobites.helpers.rememberInteractionSource
import com.sosauce.sweetselect.SweetSelectState

@Composable
fun <T> SelectedBarSurface(
    modifier: Modifier = Modifier,
    items: List<T>,
    multiSelectState: SweetSelectState<T>,
    actions: @Composable (RowScope.() -> Unit)
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(0.85f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = multiSelectState::clearSelected,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null
                )
                Spacer(Modifier.width(5.dp))
                Text("${multiSelectState.selectedItems.size}")
            }
//            Button(
//                onClick = {
//                    if (multiSelectState.selectedItems.size == items.size) {
//                        multiSelectState.clearSelected()
//                    } else {
//                        multiSelectState.toggleAll(items)
//                    }
//                },
//                shapes = ButtonDefaults.shapes(),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
//                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
//                )
//
//            ) {
//
//                val icon =
//                    if (items.size == multiSelectState.selectedItems.size) R.drawable.unselect_all else R.drawable.select_all
//
//                Icon(
//                    painter = painterResource(icon),
//                    contentDescription = null
//                )
//                Spacer(Modifier.width(5.dp))
//
//                val text =
//                    if (items.size == multiSelectState.selectedItems.size) R.string.unselect_all else R.string.select_all
//
//                Text(stringResource(text))
//            }
        }
        Spacer(Modifier.height(5.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) { actions() }
    }
}


/**
 * Action shown as a button in a selected bar
 *
 * @param tint tint of the icon, the default content color when null
 */
data class SelectedBarAction(
    @DrawableRes val icon: Int,
    val tint: Color? = null,
    val onClick: () -> Unit
)

/**
 * Connected buttons of a selected bar, one per action
 */
@Composable
fun SelectedBarActions(
    actions: List<SelectedBarAction>
) {
    val interactionSources = List(actions.size) { rememberInteractionSource() }

    ButtonGroup(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        overflowIndicator = {}
    ) {
        actions.forEachIndexed { index, action ->
            customItem(
                buttonGroupContent = {
                    Button(
                        onClick = action.onClick,
                        interactionSource = interactionSources[index],
                        shape = RoundedCornerShape(
                            topStart = if (index == 0) 50.dp else 4.dp,
                            bottomStart = if (index == 0) 50.dp else 4.dp,
                            topEnd = if (index == actions.lastIndex) 50.dp else 4.dp,
                            bottomEnd = if (index == actions.lastIndex) 50.dp else 4.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                        ),
                        modifier = Modifier
                            .animateWidth(interactionSources[index])
                            .weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(action.icon),
                            contentDescription = null,
                            tint = action.tint ?: LocalContentColor.current
                        )
                    }
                },
                menuContent = {}
            )
        }
    }
}

@Composable
fun ConversationsSelectedBar(
    modifier: Modifier = Modifier,
    items: List<CuteConversation>,
    multiSelectState: SweetSelectState<CuteConversation>,
    onDeleteConversations: () -> Unit,
    onPinThreads: () -> Unit,
    onArchiveThreads: () -> Unit
) {

    var showDeleteConversationsDialog by remember { mutableStateOf(false) }
    val screen = LocalScreen.current

    if (showDeleteConversationsDialog) {
        DeleteConversationsDialog(
            onDismissRequest = { showDeleteConversationsDialog = false },
            onDelete = {
                onDeleteConversations()
                showDeleteConversationsDialog = false
            },
            numberOfConversations = multiSelectState.selectedItems.size
        )
    }

    SelectedBarSurface(
        modifier = modifier,
        items = items,
        multiSelectState = multiSelectState
    ) {
        SelectedBarActions(
            actions = listOf(
                SelectedBarAction(
                    icon = R.drawable.pin_filled,
                    onClick = onPinThreads
                ),
                SelectedBarAction(
                    icon = if (screen is Screen.ArchivedThreads) R.drawable.unarchive else R.drawable.archive,
                    onClick = onArchiveThreads
                ),
                SelectedBarAction(
                    icon = R.drawable.delete_filled,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteConversationsDialog = true }
                )
            )
        )
    }
}

@Composable
fun ContactsSelectedBar(
    modifier: Modifier = Modifier,
    items: List<CuteContact>,
    multiSelectState: SweetSelectState<CuteContact>,
    onToggleFavorite: () -> Unit,
    onDeleteContacts: () -> Unit
) {
    SelectedBarSurface(
        modifier = modifier,
        items = items,
        multiSelectState = multiSelectState
    ) {
        SelectedBarActions(
            actions = listOf(
                SelectedBarAction(
                    icon = R.drawable.favorite_filled,
                    onClick = onToggleFavorite
                ),
                SelectedBarAction(
                    icon = R.drawable.delete_filled,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteContacts
                )
            )
        )
    }
}
