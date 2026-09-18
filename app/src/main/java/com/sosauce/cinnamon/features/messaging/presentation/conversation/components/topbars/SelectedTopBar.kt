@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.sweetselect.SweetSelectState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SelectedTopBar(
    sweetSelectState: SweetSelectState<CuteMessage>,
    onSelectAll: () -> Unit,
    onUnselectAll: () -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
) {

    val clipboard = LocalClipboard.current
    var showDeleteMsgDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showDeleteMsgDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMsgDialog = false },
            title = {
                Text(
                    pluralStringResource(
                        R.plurals.delete_msg,
                        sweetSelectState.selectedItems.size,
                        sweetSelectState.selectedItems.size
                    ),
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.are_you_sure_irreversible)
                )
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete_filled),
                    contentDescription = null
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHandleConversationActions(
                            ConversationActions.DeleteSelectedMessages(
                                sweetSelectState.selectedItems.toList()
                            )
                        )
                        sweetSelectState.clearSelected()
                        showDeleteMsgDialog = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteMsgDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    HorizontalFloatingToolbar(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .statusBarsPadding()
            .clip(FloatingToolbarDefaults.ContainerShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = Color.Transparent
        ),
        expanded = false
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onUnselectAll,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null
                )
            }
            Text(sweetSelectState.selectedItems.size.toString())
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(sweetSelectState.selectedItems.size == 1) {
                IconButton(
                    onClick = {
                        val message = sweetSelectState.selectedItems.firstOrNull()?.body ?: return@IconButton
                        scope.launch(Dispatchers.IO) {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        "",
                                        message
                                    )
                                )
                            )
                        }
                        onUnselectAll()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.copy_filled),
                        contentDescription = null
                    )
                }
            }
            IconButton(
                onClick = onSelectAll,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.select_all),
                    contentDescription = null
                )
            }
            IconButton(
                onClick = { showDeleteMsgDialog = true },
                shapes = IconButtonDefaults.shapes()
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