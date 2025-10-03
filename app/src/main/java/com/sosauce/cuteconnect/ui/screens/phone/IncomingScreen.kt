@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.phone

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sosauce.cuteconnect.data.actions.CallAction
import com.sosauce.cuteconnect.domain.states.CallUiState
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import com.sosauce.cuteconnect.ui.theme.CuteConnectTheme
import com.sosauce.cuteconnect.utils.getContactNameOrNothing
import com.sosauce.cuteconnect.viewModels.CallViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun IncomingScreen(
    onCallActions: (CallAction) -> Unit,
    callUiState: CallUiState
) {
    val context = LocalContext.current
    val interactionSources = List(2) { remember { MutableInteractionSource() } }
    val displayName = remember(callUiState.number) {
        callUiState.number.getContactNameOrNothing(context)
    }

    Scaffold { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(10.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DefaultContactIcon(
                firstLetter = displayName.firstOrNull(),
                size = 200.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialShapes.Cookie12Sided.toShape()
            )
            CuteText(
                text = displayName,
                style = MaterialTheme.typography.headlineLargeEmphasized.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Spacer(Modifier.weight(1f))
            ButtonGroup(
                overflowIndicator = {},
                modifier = Modifier.padding(bottom = 50.dp)
            ) {
                customItem(
                    {
                        IconButton(
                            onClick = { onCallActions(CallAction.DeclineCall) },
                            interactionSource = interactionSources[0],
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.error)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                                .animateWidth(interactionSources[0])
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
                        IconButton(
                            onClick = { onCallActions(CallAction.AnswerCall) },
                            interactionSource = interactionSources[1],
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryFixed,
                                contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.tertiaryFixed)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                                .animateWidth(interactionSources[1])
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Phone,
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

@Preview
@Composable
private fun IncomingPreview() {
    CuteConnectTheme {
        Scaffold { _ ->
            IncomingScreen(
                onCallActions = {},
                callUiState = CallUiState(number = "0767982150")
            )
        }
    }
}
