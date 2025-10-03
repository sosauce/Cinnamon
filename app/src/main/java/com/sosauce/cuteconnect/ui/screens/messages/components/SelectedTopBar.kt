@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cuteconnect.ui.screens.messages.components

import android.content.ClipData
import android.provider.Telephony
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.data.actions.CommonAction
import com.sosauce.cuteconnect.domain.model.CuteMessage
import com.sosauce.cuteconnect.ui.navigation.LocalHazeState
import com.sosauce.cuteconnect.ui.shared_components.sheets.DeleteMessageSheet
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SelectedTopBar(
    selectedCuteMessages: List<CuteMessage>,
    onUnselectAll: () -> Unit,
    onHandleCommonAction: (CommonAction) -> Unit,
) {

    val clipboard = LocalClipboard.current
    var showDeleteMsgDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showDeleteMsgDialog) {
        DeleteMessageSheet(
            onDismissRequest = { showDeleteMsgDialog = false },
            selectedMessages = selectedCuteMessages.size,
            onDelete = {
                selectedCuteMessages.fastForEach { cuteMessage ->
                    onHandleCommonAction(
                        CommonAction.DeleteFromContentUri(
                            Telephony.Sms.CONTENT_URI,
                            cuteMessage.id
                        )
                    )
                }
                onUnselectAll()
                showDeleteMsgDialog = false
            }
        )
    }
    HorizontalFloatingToolbar(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .statusBarsPadding()
            .clip(FloatingToolbarDefaults.ContainerShape)
            .hazeEffect(
                state = LocalHazeState.current,
                style = HazeMaterials.regular(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ),
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
                onClick = onUnselectAll
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null
                )
            }
            CuteText(selectedCuteMessages.size.toString())
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(selectedCuteMessages.size == 1) {
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText("",selectedCuteMessages.first().body)
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
                onClick = { showDeleteMsgDialog = true }
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