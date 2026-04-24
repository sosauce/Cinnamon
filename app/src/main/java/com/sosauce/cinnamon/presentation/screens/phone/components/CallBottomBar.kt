@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.phone.components

import android.telecom.CallAudioState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.states.CallState
import com.sosauce.cinnamon.domain.states.DialerPaneContent
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.screens.phone.CallingState
import com.sosauce.cinnamon.utils.selfAlignHorizontally

@Composable
fun CallBottomBar(
    onCallAction: (CallAction) -> Unit,
    callUiState: CallingState
) {

    val interactionSources = List(4) { remember { MutableInteractionSource() } }
    var paneContent by remember { mutableStateOf(DialerPaneContent.NOTHING) }
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .selfAlignHorizontally()
            .fillMaxWidth(0.9f)
    ) {
        AnimatedVisibility(
            visible = paneContent != DialerPaneContent.NOTHING,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {

            when (paneContent) {
                DialerPaneContent.DIALPAD -> {
                    Dialpad(
                        onSendTone = { onCallAction(CallAction.StartTone(it)) },
                    )
                }
                DialerPaneContent.AUDIO_SWITCHER -> {
                    AudioSwitcher(
                        onCallAction = onCallAction,
                        routes = callUiState.availableAudioRoutes
                    )
                }
                DialerPaneContent.NOTHING -> Unit
            }

        }
        Spacer(Modifier.height(5.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(
                    horizontal = 5.dp,
                    vertical = 10.dp
                )
        ) {
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ToggleButton(
                    checked = callUiState.isMuted,
                    onCheckedChange = { onCallAction(CallAction.ToggleMute(!callUiState.isMuted)) },
                    enabled = callUiState.callState != CallState.ENDED,
                    interactionSource = interactionSources[0],
                    shapes = ToggleButtonDefaults.shapes(),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                        .animateWidth(interactionSources[0])
                ) {
                    Icon(
                        painter = if (callUiState.isMuted) painterResource(R.drawable.mic_off) else painterResource(R.drawable.mic),
                        contentDescription = null
                    )
                }
                ToggleButton(
                    checked = callUiState.currentAudioRoute.type != CallAudioState.ROUTE_EARPIECE,
                    onCheckedChange = {
                        paneContent = if (paneContent == DialerPaneContent.AUDIO_SWITCHER) {
                            DialerPaneContent.NOTHING
                        } else DialerPaneContent.AUDIO_SWITCHER
                    },
                    enabled = callUiState.callState != CallState.ENDED,
                    interactionSource = interactionSources[1],
                    shapes = ToggleButtonDefaults.shapes(),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                        .animateWidth(interactionSources[1])
                ) {
                    AnimatedContent(
                        targetState = callUiState.currentAudioRoute.type.routeToIcon()
                    ) {
                        Icon(
                            painter = painterResource(it),
                            contentDescription = null
                        )
                    }
                }
                ToggleButton(
                    checked = callUiState.isHolding,
                    onCheckedChange = { onCallAction(CallAction.ToggleHold) },
                    enabled = callUiState.callState != CallState.ENDED,
                    interactionSource = interactionSources[2],
                    shapes = ToggleButtonDefaults.shapes(),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                        .animateWidth(interactionSources[2])
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pause_filled),
                        contentDescription = null
                    )
                }
                ToggleButton(
                    checked = paneContent == DialerPaneContent.DIALPAD,
                    onCheckedChange = {
                        paneContent = if (paneContent != DialerPaneContent.DIALPAD) {
                            DialerPaneContent.DIALPAD
                        } else {
                            DialerPaneContent.NOTHING
                        }
                    },
                    enabled = callUiState.callState != CallState.ENDED,
                    interactionSource = interactionSources[3],
                    shapes = ToggleButtonDefaults.shapes(),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                        .animateWidth(interactionSources[3])
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dialpad),
                        contentDescription = null
                    )
                }
            }
            //Spacer(Modifier.height(10.dp))
            IconButton(
                onClick = { onCallAction(CallAction.HangUp) },
                enabled = callUiState.callState != CallState.ENDED,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.error)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
            ) {
                Icon(
                    painter = painterResource(R.drawable.phone_filled),
                    contentDescription = null,
                    modifier = Modifier.rotate(135f)
                )
            }
        }
    }

}