@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import com.sosauce.cinnamon.features.phone.presentation.call.CallingState

/**
 * M3 Expressive call bubble — shown over other apps when call is ongoing/dialing/ringing
 * Blur background via tonal surface + semi-transparent scrim, pill shape extraLargeIncreased
 */
@Composable
fun CallBubble(
    state: CallingState,
    onExpand: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRinging = state.callState == CallState.RINGING
    val isOngoing = state.callState == CallState.ONGOING
    val isDialing = state.callState == CallState.DIALING

    // Expressive tonal container with blur-like semi-transparent surface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(28.dp), // extraLarge expressive
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        // Inner blur scrim — subtle gradient for depth
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar — expressive squircle
                Box(contentAlignment = Alignment.Center) {
                    DefaultContactIcon(
                        firstLetter = state.displayName.firstOrNull(),
                        size = 48.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialShapes.Cookie9Sided.toShape(),
                        contactPhoneNumber = state.number,
                        modifier = Modifier
                    )
                    // Pulsing dot for ongoing/ringing
                    if (isRinging || isDialing || isOngoing) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isRinging -> MaterialTheme.colorScheme.tertiary
                                        isOngoing -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                )
                                .padding(2.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isRinging -> MaterialTheme.colorScheme.tertiary
                                            isOngoing -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }

                // Caller info — emphasized typography
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = state.displayName.ifBlank { state.number.ifBlank { "Unknown" } },
                        style = MaterialTheme.typography.titleMediumEmphasized.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedContent(
                        targetState = when (state.callState) {
                            CallState.RINGING -> "Incoming • via ${state.activeSim.name.ifBlank { "SIM" }}"
                            CallState.DIALING -> "Calling…"
                            CallState.ONGOING -> formatDuration(state.timeSpentInCall)
                            CallState.ENDED -> "Ended"
                            else -> state.number
                        },
                        transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                        label = "bubbleStatus"
                    ) { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelMediumEmphasized.copy(
                                color = when (state.callState) {
                                    CallState.RINGING -> MaterialTheme.colorScheme.tertiary
                                    CallState.ONGOING -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Actions — expressive tonal buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute — only when ongoing
                    if (isOngoing) {
                        FilledTonalIconButton(
                            onClick = onToggleMute,
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (state.isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (state.isMuted) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(if (state.isMuted) R.drawable.mic_off else R.drawable.mic),
                                contentDescription = "Mute",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // End call — always visible, error pill
                    FilledIconButton(
                        onClick = onEndCall,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.phone_filled),
                            contentDescription = "End call",
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(135f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
