@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars

import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
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
    onNavigate: (Screen) -> Unit,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .selfAlignHorizontally()
            .fillMaxWidth(0.95f)
            .statusBarsPadding()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(6.dp)
    ) {
        Button(
            onClick = onUnselectAll,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            AnimatedContent(
                targetState = sweetSelectState.selectedItems.size,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = sweetSelectState.selectedItems.size == 1,
            enter = scaleIn(bouncySpec()) + fadeIn(),
            exit = scaleOut(bouncySpec()) + fadeOut()
        ) {
            Row {
                IconButton(
                    onClick = {
                        val message = sweetSelectState.selectedItems.firstOrNull()?.body ?: return@IconButton
                        println("forward, selected top bar: $message")
                        onNavigate(
                            Screen.Forwarding(message)
                        )
                    },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.forward),
                        contentDescription = null
                    )
                }
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
                    },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.copy_filled),
                        contentDescription = null
                    )
                }
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
        FilledIconButton(
            onClick = { showDeleteMsgDialog = true },
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.size(
                IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.delete_filled),
                contentDescription = null
            )
        }
    }
}
