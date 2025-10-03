@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)

package com.sosauce.cuteconnect.ui.screens.messages.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingActions
import com.sosauce.cuteconnect.data.datastore.rememberDefaultSimCard
import com.sosauce.cuteconnect.domain.model.ConversationSettings
import com.sosauce.cuteconnect.domain.model.CuteSimCard
import com.sosauce.cuteconnect.ui.navigation.LocalHazeState
import com.sosauce.cuteconnect.ui.shared_components.SimSelector
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.toolbars.ToolbarSkeleton
import com.sosauce.cuteconnect.utils.addOrNot
import com.sosauce.cuteconnect.utils.rememberSearchbarMaxFloatValue
import com.sosauce.cuteconnect.utils.rememberSearchbarRightPadding
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@Composable
fun ConversationBottomBar(
    modifier: Modifier = Modifier,
    onSaveDraft: (String) -> Unit,
    onSendMessage: (message: String) -> Unit,
    cuteSimCards: List<CuteSimCard>,
) {

    val textFieldState = rememberTextFieldState()
    val mediasToSend = rememberSaveable { mutableStateListOf<Uri>() }
    var isActionPickerExpanded by remember { mutableStateOf(false) }
    val defaultSimCard by rememberDefaultSimCard()
    var simSelectorVisible by remember { mutableStateOf(false) }

    SimSelector(
        visible = simSelectorVisible,
        onDismissRequest = { simSelectorVisible = false },
        cuteSimCards = cuteSimCards
    )

    DisposableEffect(Unit) {
        onDispose {
            if (textFieldState.text.isNotEmpty()) {
                onSaveDraft(textFieldState.text.toString())
            }

        }
    }

    ActionPicker(
        expanded = isActionPickerExpanded,
        onDismissRequest = { isActionPickerExpanded = false },
        onUpdateMediasToSend = { mediasToSend.addOrNot(it) }
    )

    ToolbarSkeleton(
        modifier = Modifier.imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextField(
                state = textFieldState,
                placeholder = {
                    CuteText(
                        text = "Message",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = TextFieldDefaults.colors(
                    disabledIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    IconButton(
                        onClick = { isActionPickerExpanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )
                    }
                },
                trailingIcon = {
                    val defaultSim = cuteSimCards.fastFirstOrNull { it.subId == defaultSimCard }

                    IconButton(
                        onClick = { simSelectorVisible = true }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SimCard,
                            contentDescription = null,
                            tint = Color(defaultSim?.color ?: 0)
                        )
                    }
                },
                shape = FloatingToolbarDefaults.ContainerShape,
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)

            )
            IconButton(
                onClick = {
                    onSendMessage(textFieldState.text.toString())
                    textFieldState.clearText()
                },
                enabled = textFieldState.text.isNotEmpty() && textFieldState.text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null
                )
            }
        }
    }
}