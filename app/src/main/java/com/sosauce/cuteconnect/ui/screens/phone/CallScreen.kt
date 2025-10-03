package com.sosauce.cuteconnect.ui.screens.phone

import android.telecom.CallAudioState
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.sosauce.cuteconnect.data.actions.CallAction
import com.sosauce.cuteconnect.domain.states.CallState
import com.sosauce.cuteconnect.domain.states.CallUiState
import com.sosauce.cuteconnect.domain.states.DialerPaneContent
import com.sosauce.cuteconnect.ui.screens.phone.components.AudioSwitcher
import com.sosauce.cuteconnect.ui.screens.phone.components.Dialpad
import com.sosauce.cuteconnect.ui.screens.phone.components.routeToIcon
import com.sosauce.cuteconnect.utils.ImageUtils
import com.sosauce.cuteconnect.utils.getContactNameOrNothing
import com.sosauce.cuteconnect.utils.toReadableTime
import com.sosauce.cuteconnect.utils.toStopwatch
import com.sosauce.cuteconnect.viewModels.CallViewModel
import dev.chrisbanes.haze.hazeSource
import kotlin.time.DurationUnit

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallScreen(
    onCallAction: (CallAction) -> Unit,
    callUiState: CallUiState
) {

    val context = LocalContext.current
    val interactionSources = List(3) { remember { MutableInteractionSource() } }
    val interactionSources2 = List(3) { remember { MutableInteractionSource() } }
    val displayName = remember(callUiState.number) {
        callUiState.number.getContactNameOrNothing(context)
    }
    var paneContent by remember { mutableStateOf(DialerPaneContent.NOTHING) }
    Box {

        AsyncImage(
            model = ImageUtils.imageRequester(callUiState.poster.toUri(), context),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                DefaultContactIcon(
                    firstLetter = displayName.firstOrNull(),
                    size = 150.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                )
                Spacer(Modifier.height(20.dp))
                CuteText(
                    text = when(callUiState.callState) {
                        CallState.RINGING, CallState.DIALING  -> "Ringing..."
                        CallState.ONGOING -> displayName
                        CallState.ENDED -> "Call ended"
                    },
                    fontSize = 30.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(Modifier.height(10.dp))
                if (callUiState.callState == CallState.DIALING) {
                    ContainedLoadingIndicator()
                } else {
                    CuteText(
                        text = callUiState.timeSpentInCall.toStopwatch(DurationUnit.SECONDS),
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(
                    visible = callUiState.isHolding,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    CuteText(
                        text = "On hold",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 20.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Column {
                    AnimatedVisibility(
                        visible = paneContent != DialerPaneContent.NOTHING,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
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
                    ButtonGroup(
                        overflowIndicator = {}
                    ) {
                        customItem(
                            {
                                ToggleButton(
                                    checked = callUiState.isMuted,
                                    onCheckedChange = { onCallAction(CallAction.ToggleMute(!callUiState.isMuted)) },
                                    enabled = callUiState.callState != CallState.ENDED,
                                    interactionSource = interactionSources[0],
                                    shapes = ToggleButtonDefaults.shapes(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                                        .animateWidth(interactionSources[0])
                                ) {
                                    AnimatedContent(
                                        targetState = callUiState.isMuted
                                    ) { isMuted ->
                                        Icon(
                                            imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            {}
                        )
                        customItem(
                            {
                                ToggleButton(
                                    checked = callUiState.currentAudioRoute.type != CallAudioState.ROUTE_EARPIECE,
                                    onCheckedChange = { paneContent = DialerPaneContent.AUDIO_SWITCHER },
                                    enabled = callUiState.callState != CallState.ENDED,
                                    interactionSource = interactionSources[1],
                                    shapes = ToggleButtonDefaults.shapes(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                                        .animateWidth(interactionSources[1])
                                ) {
                                    AnimatedContent(
                                        targetState = callUiState.currentAudioRoute.type.routeToIcon()
                                    ) {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            {}
                        )
                        customItem(
                            {
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
                                    interactionSource = interactionSources[2],
                                    shapes = ToggleButtonDefaults.shapes(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                                        .animateWidth(interactionSources[2])
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Dialpad,
                                        contentDescription = null
                                    )
                                }
                            },
                            {}
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    ButtonGroup(
                        overflowIndicator = {}
                    ) {
                        customItem(
                            {
                                ToggleButton(
                                    checked = callUiState.isHolding,
                                    onCheckedChange = { onCallAction(CallAction.ToggleHold) },
                                    enabled = callUiState.callState != CallState.ENDED,
                                    interactionSource = interactionSources2[0],
                                    shapes = ToggleButtonDefaults.shapes(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Narrow))
                                        .animateWidth(interactionSources2[0])
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Pause,
                                        contentDescription = null
                                    )
                                }
                            },
                            {}
                        )
                        customItem(
                            {
                                IconButton(
                                    onClick = { onCallAction(CallAction.HangUp) },
                                    interactionSource = interactionSources2[1],
                                    enabled = callUiState.callState != CallState.ENDED,
                                    shapes = IconButtonDefaults.shapes(),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.errorContainer)
                                    ),
                                    modifier = Modifier
                                        .weight(2f)
                                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                                        .animateWidth(interactionSources2[1])
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(135f)
                                    )
                                }
                            },
                            {}
                        )
                        customItem(
                            {
                                FilledTonalIconButton(
                                    onClick = {  },
                                    interactionSource = interactionSources2[2],
                                    enabled = callUiState.callState != CallState.ENDED,
                                    shapes = IconButtonDefaults.shapes(),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Narrow))
                                        .animateWidth(interactionSources2[2])
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null
                                    )
                                }
                            },
                            {}
                        )
                    }
                }
            }
        }
    }
}