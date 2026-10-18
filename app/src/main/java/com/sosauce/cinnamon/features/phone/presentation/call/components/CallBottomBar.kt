package com.sosauce.cinnamon.features.phone.presentation.call.components

import android.telecom.CallAudioState
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import com.sosauce.cinnamon.features.phone.presentation.call.CallingState
import com.sosauce.cinnamon.features.phone.presentation.call.DialerPaneContent
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.animations.bouncySpec

@Composable
fun CallBottomBar(
    onCallAction: (CallAction) -> Unit,
    callUiState: CallingState
) {

    val interactionSources = List(4) { remember { MutableInteractionSource() } }
    var paneContent by remember { mutableStateOf(DialerPaneContent.NOTHING) }

    val isEnded = callUiState.callState == CallState.ENDED
    val isHolding = callUiState.isHolding
    val isMuted = callUiState.isMuted

    val actions = listOf(
        CallAction(
            onClick = { onCallAction(CallAction.ToggleMute(!isMuted)) },
            isActive = isMuted,
            icon = if (isMuted) R.drawable.mic_off else R.drawable.mic_off,
            label = if (isMuted) R.string.unmute else R.string.mute
        ),
        CallAction(
            onClick = {
                paneContent = if (paneContent == DialerPaneContent.AUDIO_SWITCHER) {
                    DialerPaneContent.NOTHING
                } else DialerPaneContent.AUDIO_SWITCHER
            },
            isActive = callUiState.currentAudioRoute.type != CallAudioState.ROUTE_EARPIECE || paneContent == DialerPaneContent.AUDIO_SWITCHER,
            icon = callUiState.currentAudioRoute.type.routeToIcon(),
            label = R.string.audio
        ),
        CallAction(
            onClick = { onCallAction(CallAction.ToggleMute(isMuted)) },
            isActive = isHolding,
            icon = R.drawable.pause_filled,
            label = R.string.hold
        ),
        CallAction(
            onClick = {
                paneContent = if (paneContent != DialerPaneContent.DIALPAD) {
                    DialerPaneContent.DIALPAD
                } else {
                    DialerPaneContent.NOTHING
                }
            },
            isActive = paneContent == DialerPaneContent.DIALPAD,
            icon = R.drawable.dialpad,
            label = R.string.dialpad
        )

    )




    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .selfAlignHorizontally()
            .fillMaxWidth(0.96f)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedContent(
            targetState = paneContent,
            transitionSpec = {
                (slideInVertically(bouncySpec()) { it / 2 } + fadeIn() + scaleIn(bouncySpec(), initialScale = 0.96f)) togetherWith
                    (slideOutVertically(bouncySpec()) { it / 2 } + fadeOut() + scaleOut(bouncySpec(), targetScale = 0.96f))
            }
        ) { target ->
            when (target) {
                DialerPaneContent.NOTHING -> Unit
                DialerPaneContent.DIALPAD, DialerPaneContent.AUDIO_SWITCHER -> {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                        ) {
                            when (target) {
                                DialerPaneContent.DIALPAD -> Dialpad(
                                    onSendTone = { onCallAction(CallAction.StartTone(it)) },
                                )
                                DialerPaneContent.AUDIO_SWITCHER -> AudioSwitcher(
                                    onCallAction = onCallAction,
                                    routes = callUiState.availableAudioRoutes
                                )
                            }
                        }
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    overflowIndicator = {}
                ) {
                    actions.fastForEachIndexed { index, action ->
                        customItem(
                            buttonGroupContent = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ToggleButton(
                                        checked = action.isActive,
                                        onCheckedChange = { action.onClick() },
                                        enabled = !isEnded,
                                        interactionSource = interactionSources[index],
                                        colors = ToggleButtonDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .animateWidth(interactionSources[index])
                                    ) {
                                        Icon(
                                            painter = painterResource(action.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(
                                        text = stringResource(action.label),
                                        style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            },
                            menuContent = {}
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onCallAction(CallAction.HangUp) },
                        enabled = !isEnded,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        ),
                        contentPadding = ButtonDefaults.ContentPadding,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.phone_filled),
                            contentDescription = null,
                            modifier = Modifier
                                .rotate(135f)
                                .size(24.dp)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(if (isEnded) R.string.call_ended else R.string.hang_up),
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                    }
                }
            }
        }
    }
}

private data class CallAction(
    val onClick: () -> Unit,
    val isActive: Boolean,
    @DrawableRes val icon: Int,
    @StringRes val label: Int
)
