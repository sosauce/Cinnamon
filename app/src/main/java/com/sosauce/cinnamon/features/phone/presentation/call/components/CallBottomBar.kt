@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call.components

import android.telecom.CallAudioState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import com.sosauce.cinnamon.features.phone.presentation.call.CallingState
import com.sosauce.cinnamon.features.phone.presentation.call.DialerPaneContent
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally

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

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .selfAlignHorizontally()
            .fillMaxWidth(0.96f)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expressive expanding pane — dialpad / audio switcher
        AnimatedContent(
            targetState = paneContent,
            transitionSpec = {
                (slideInVertically(bouncySpec()) { it / 2 } + fadeIn() + scaleIn(bouncySpec(), initialScale = 0.96f)) togetherWith
                    (slideOutVertically(bouncySpec()) { it / 2 } + fadeOut() + scaleOut(bouncySpec(), targetScale = 0.96f))
            },
            label = "paneContent"
        ) { target ->
            when (target) {
                DialerPaneContent.NOTHING -> Box(Modifier.fillMaxWidth().height(0.dp))
                DialerPaneContent.DIALPAD, DialerPaneContent.AUDIO_SWITCHER -> {
                    Surface(
                        shape = RoundedCornerShape(28.dp), // extraLarge expressive
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(28.dp)
                                )
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
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        // Main expressive control surface — tonal container with largeIncreased corners
        Surface(
            shape = RoundedCornerShape(32.dp), // extraLargeIncreased expressive
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
                // Expressive ButtonGroup — 4 toggle controls with expressive shapes + motion
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // 8dp spacing system
                    overflowIndicator = {}
                ) {
                    // 1) Mute — tonal toggle with expressive pill morph
                    customItem(
                        buttonGroupContent = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ToggleButton(
                                    checked = isMuted,
                                    onCheckedChange = { onCallAction(CallAction.ToggleMute(!isMuted)) },
                                    enabled = !isEnded,
                                    interactionSource = interactionSources[0],
                                    colors = ToggleButtonDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .animateWidth(interactionSources[0])
                                ) {
                                    Icon(
                                        painter = if (isMuted) painterResource(R.drawable.mic_off) else painterResource(R.drawable.mic),
                                        contentDescription = if (isMuted) "Unmute" else "Mute",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = if (isMuted) "Unmute" else "Mute",
                                    style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        menuContent = {}
                    )

                    // 2) Audio Route — shows current route icon with expressive morph
                    customItem(
                        buttonGroupContent = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ToggleButton(
                                    checked = callUiState.currentAudioRoute.type != CallAudioState.ROUTE_EARPIECE ||
                                        paneContent == DialerPaneContent.AUDIO_SWITCHER,
                                    onCheckedChange = {
                                        paneContent = if (paneContent == DialerPaneContent.AUDIO_SWITCHER) {
                                            DialerPaneContent.NOTHING
                                        } else DialerPaneContent.AUDIO_SWITCHER
                                    },
                                    enabled = !isEnded,
                                    interactionSource = interactionSources[1],
                                    colors = ToggleButtonDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .animateWidth(interactionSources[1])
                                ) {
                                    AnimatedContent(
                                        targetState = callUiState.currentAudioRoute.type.routeToIcon(),
                                        label = "routeIcon"
                                    ) { icon ->
                                        Icon(
                                            painter = painterResource(icon),
                                            contentDescription = "Audio",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Speaker",
                                    style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        menuContent = {}
                    )

                    // 3) Hold
                    customItem(
                        buttonGroupContent = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ToggleButton(
                                    checked = isHolding,
                                    onCheckedChange = { onCallAction(CallAction.ToggleHold) },
                                    enabled = !isEnded,
                                    interactionSource = interactionSources[2],
                                    colors = ToggleButtonDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .animateWidth(interactionSources[2])
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.pause_filled),
                                        contentDescription = "Hold",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = "Hold",
                                    style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isHolding) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        menuContent = {}
                    )

                    // 4) Dialpad
                    customItem(
                        buttonGroupContent = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ToggleButton(
                                    checked = paneContent == DialerPaneContent.DIALPAD,
                                    onCheckedChange = {
                                        paneContent = if (paneContent != DialerPaneContent.DIALPAD) {
                                            DialerPaneContent.DIALPAD
                                        } else {
                                            DialerPaneContent.NOTHING
                                        }
                                    },
                                    enabled = !isEnded,
                                    interactionSource = interactionSources[3],
                                    colors = ToggleButtonDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .animateWidth(interactionSources[3])
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.dialpad),
                                        contentDescription = "Dialpad",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = "Keypad",
                                    style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        menuContent = {}
                    )
                }

                // Expressive Hang-up — large, pill, error tonal with motion
                // 8dp gap above per spacing system
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Optional add-call placeholder for future — kept as subtle outline for symmetry on large screens
                    // Primary hangup takes full width expressive shape
                    Button(
                        onClick = { onCallAction(CallAction.HangUp) },
                        enabled = !isEnded,
                        shapes = ButtonDefaults.shapes(
                            shape = RoundedCornerShape(50), // Full pill expressive
                            pressedShape = RoundedCornerShape(16.dp)
                        ),
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
                            contentDescription = "Hang up",
                            modifier = Modifier
                                .rotate(135f)
                                .size(24.dp)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = if (isEnded) "Ended" else "End call",
                            style = MaterialTheme.typography.titleMediumEmphasized.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
        }
    }
}
