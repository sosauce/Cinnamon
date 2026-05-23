@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.shared_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.messages.components.dialogs.DeleteConversationsDialog
import com.sosauce.cinnamon.utils.LocalScreen
import com.sosauce.cinnamon.utils.rememberInteractionSource
import com.sosauce.cinnamon.utils.rememberSearchbarMaxFloatValue
import com.sosauce.cinnamon.utils.rememberSearchbarRightPadding
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
            .fillMaxWidth(rememberSearchbarMaxFloatValue())
            .padding(end = rememberSearchbarRightPadding())
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
            Button(
                onClick = {
                    if (multiSelectState.selectedItems.size == items.size) {
                        multiSelectState.clearSelected()
                    } else {
                        multiSelectState.toggleAll(items)
                    }
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                )

            ) {

                val icon =
                    if (items.size == multiSelectState.selectedItems.size) R.drawable.unselect_all else R.drawable.select_all

                Icon(
                    painter = painterResource(icon),
                    contentDescription = null
                )
                Spacer(Modifier.width(5.dp))

                val text =
                    if (items.size == multiSelectState.selectedItems.size) R.string.unselect_all else R.string.select_all

                Text(stringResource(text))
            }
        }
        Spacer(Modifier.height(5.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) { actions() }
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
    val interactionSources = List(3) { rememberInteractionSource() }
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
        ButtonGroup(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Button(
                onClick = onPinThreads,
                interactionSource = interactionSources[0],
                shape = RoundedCornerShape(
                    topStart = 50.dp,
                    bottomStart = 50.dp,
                    topEnd = 4.dp,
                    bottomEnd = 4.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                ),
                modifier = Modifier
                    .animateWidth(interactionSources[0])
                    .weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.pin_filled),
                    contentDescription = null
                )
            }
            Button(
                onClick = onArchiveThreads,
                interactionSource = interactionSources[1],
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                ),
                modifier = Modifier
                    .animateWidth(interactionSources[1])
                    .weight(1f)
            ) {
                val icon =
                    if (screen is Screen.ArchivedThreads) R.drawable.unarchive else R.drawable.archive

                Icon(
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }
            Button(
                onClick = { showDeleteConversationsDialog = true },
                interactionSource = interactionSources[2],
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    bottomStart = 4.dp,
                    topEnd = 50.dp,
                    bottomEnd = 50.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                ),
                modifier = Modifier
                    .animateWidth(interactionSources[2])
                    .weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
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

    val interactionSources = List(2) { rememberInteractionSource() }

    SelectedBarSurface(
        modifier = modifier,
        items = items,
        multiSelectState = multiSelectState
    ) {
        ButtonGroup(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Button(
                onClick = onToggleFavorite,
                interactionSource = interactionSources[0],
                shape = RoundedCornerShape(
                    topStart = 50.dp,
                    bottomStart = 50.dp,
                    topEnd = 4.dp,
                    bottomEnd = 4.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                ),
                modifier = Modifier
                    .animateWidth(interactionSources[0])
                    .weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.favorite_filled),
                    contentDescription = null
                )
            }
            Button(
                onClick = onDeleteContacts,
                interactionSource = interactionSources[1],
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    bottomStart = 4.dp,
                    topEnd = 50.dp,
                    bottomEnd = 50.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                ),
                modifier = Modifier
                    .animateWidth(interactionSources[1])
                    .weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}