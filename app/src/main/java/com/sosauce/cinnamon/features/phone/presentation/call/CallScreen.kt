package com.sosauce.cinnamon.features.phone.presentation.call

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.providers.PhotoQuality
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.utils.toStopwatch
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.presentation.call.components.CallBottomBar
import com.sosauce.cinnamon.features.phone.presentation.call.components.IncomingBottomBar
import com.sosauce.nekobites.animations.bouncySpec
import kotlin.time.DurationUnit

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallScreen(
    onCallAction: (CallAction) -> Unit,
    callUiState: CallingState
) {
    val isRinging = callUiState.callState == CallState.RINGING
    val isDialing = callUiState.callState == CallState.DIALING

    // Expressive pulse for incoming / outgoing
    val infinite = rememberInfiniteTransition(label = "callPulse")
    val pulseScale1 by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isRinging || isDialing) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulseScale2 by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isRinging || isDialing) 1.32f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )
    val pulseAlpha1 = if (isRinging || isDialing) 0.18f else 0f
    val pulseAlpha2 = if (isRinging || isDialing) 0.09f else 0f

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            AnimatedContent(
                targetState = isRinging,
                transitionSpec = { scaleIn(bouncySpec()) + fadeIn() togetherWith scaleOut(bouncySpec()) + fadeOut() },
                label = "bottomBarSwitch"
            ) { ringing ->
                if (ringing) {
                    IncomingBottomBar(onCallAction = onCallAction)
                } else {
                    CallBottomBar(
                        onCallAction = onCallAction,
                        callUiState = callUiState
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Poster / backdrop with expressive scrim + tonal gradient
            AsyncImage(
                model = callUiState.poster.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(
                    color = Color.Black.copy(alpha = 0.22f),
                    blendMode = BlendMode.Darken
                ),
            )
            // Expressive gradient scrim — surface tonal wash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            // Subtle radial vignette behind avatar for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top spacer for breathing room — 8dp system
                Spacer(Modifier.height(24.dp))

                // Center hero — Expressive avatar cluster
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar with expressive squircle + pulse rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        // Outer pulse rings — tonal expressive elevation cue
                        if (isRinging || isDialing) {
                            Box(
                                modifier = Modifier
                                    .size(268.dp)
                                    .scale(pulseScale2)
                                    .clip(MaterialShapes.Cookie9Sided.toShape())
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha2)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(236.dp)
                                    .scale(pulseScale1)
                                    .clip(MaterialShapes.Cookie9Sided.toShape())
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha1)
                                    )
                            )
                        }
                        // Soft shadow / tonal container behind avatar
                        Box(
                            modifier = Modifier
                                .size(196.dp)
                                .clip(MaterialShapes.Cookie9Sided.toShape())
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f))
                        )
                        DefaultContactIcon(
                            firstLetter = callUiState.displayName.firstOrNull(),
                            size = 184.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialShapes.Cookie9Sided.toShape(),
                            contactPhoneNumber = callUiState.number,
                            quality = PhotoQuality.FULL_QUALITY
                        )
                        // Expressive status dot — shows muted / holding
                        androidx.compose.animation.AnimatedVisibility(
                            visible = callUiState.isHolding || callUiState.isMuted,
                            enter = scaleIn(bouncySpec()) + fadeIn(),
                            exit = scaleOut(bouncySpec()) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (callUiState.isHolding) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                tonalElevation = 3.dp,
                                shadowElevation = 6.dp,
                                modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (callUiState.isHolding) R.drawable.pause_filled else R.drawable.mic_off
                                    ),
                                    contentDescription = null,
                                    tint = if (callUiState.isHolding) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }

                    // Name + number — emphasized expressive typography
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = callUiState.displayName.ifBlank { callUiState.number.ifBlank { stringResource(R.string.unknown) } },
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineLargeEmphasized.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .basicMarquee()
                                .fillMaxWidth()
                        )
                        // Secondary number when displayName differs
                        if (callUiState.displayName.isNotBlank() && callUiState.number.isNotBlank() && callUiState.displayName != callUiState.number) {
                            Text(
                                text = callUiState.number,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Expressive status chip — tonal, pill, with icon
                        val secondaryText: AnnotatedString = when (callUiState.callState) {
                            CallState.RINGING -> buildAnnotatedString {
                                append(stringResource(R.string.via))
                                append(" ")
                                withStyle(SpanStyle(color = Color(callUiState.activeSim.color), fontWeight = FontWeight.Bold)) {
                                    append(callUiState.activeSim.name)
                                }
                            }
                            CallState.DIALING -> AnnotatedString(stringResource(R.string.ringing))
                            CallState.ENDED -> AnnotatedString(stringResource(R.string.call_ended))
                            CallState.ONGOING -> AnnotatedString(
                                callUiState.timeSpentInCall.toStopwatch(DurationUnit.SECONDS)
                            )
                        }

                        val chipContainer: Color
                        val chipContent: Color
                        val chipIcon: Int?
                        val chipLabel: String

                        when (callUiState.callState) {
                            CallState.RINGING -> {
                                chipContainer = MaterialTheme.colorScheme.secondaryContainer
                                chipContent = MaterialTheme.colorScheme.onSecondaryContainer
                                chipIcon = R.drawable.sim_card
                                chipLabel = secondaryText.text
                            }
                            CallState.DIALING -> {
                                chipContainer = MaterialTheme.colorScheme.tertiaryContainer
                                chipContent = MaterialTheme.colorScheme.onTertiaryContainer
                                chipIcon = R.drawable.phone
                                chipLabel = secondaryText.text
                            }
                            CallState.ENDED -> {
                                chipContainer = MaterialTheme.colorScheme.surfaceContainerHighest
                                chipContent = MaterialTheme.colorScheme.onSurfaceVariant
                                chipIcon = null
                                chipLabel = secondaryText.text
                            }
                            CallState.ONGOING -> {
                                chipContainer = MaterialTheme.colorScheme.primaryContainer
                                chipContent = MaterialTheme.colorScheme.onPrimaryContainer
                                chipIcon = R.drawable.timer
                                chipLabel = secondaryText.text
                            }
                        }

                        // Use AssistChip for full expressive pill + motion
                        // Fallback to Surface chip for annotated SIM color
                        if (callUiState.callState == CallState.RINGING) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = chipContainer,
                                tonalElevation = 2.dp,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.sim_card_filled),
                                        contentDescription = null,
                                        tint = chipContent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = secondaryText,
                                        style = MaterialTheme.typography.labelLargeEmphasized.copy(
                                            color = chipContent
                                        )
                                    )
                                }
                            }
                        } else {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = {
                                    Text(
                                        text = chipLabel,
                                        style = MaterialTheme.typography.labelLargeEmphasized
                                    )
                                },
                                leadingIcon = chipIcon?.let {
                                    {
                                        Icon(
                                            painter = painterResource(it),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = chipContainer,
                                    labelColor = chipContent,
                                    leadingIconContentColor = chipContent,
                                    disabledContainerColor = chipContainer,
                                    disabledLabelColor = chipContent,
                                    disabledLeadingIconContentColor = chipContent
                                ),
                                border = null,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // On-hold expressive banner chip
                        AnimatedVisibility(
                            visible = callUiState.isHolding,
                            enter = scaleIn(bouncySpec()) + fadeIn(),
                            exit = scaleOut(bouncySpec()) + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer,
                                tonalElevation = 2.dp,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.pause_filled),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.on_hold),
                                        style = MaterialTheme.typography.labelMediumEmphasized.copy(
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom spacer keeps avatar centered with breathing room above bottom bar
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CallScreenPreviewRinging() {
    CinnamonTheme {
        CallScreen(
            onCallAction = {},
            callUiState = CallingState(
                number = "+1 555 0100",
                displayName = "Ava Thompson",
                callState = CallState.RINGING,
                availableAudioRoutes = listOf(
                    AudioRoute(name = "Speaker"),
                    AudioRoute(name = "Earpiece")
                )
            )
        )
    }
}

@Preview
@Composable
private fun CallScreenPreviewOngoing() {
    CinnamonTheme {
        CallScreen(
            onCallAction = {},
            callUiState = CallingState(
                number = "+1 555 0100",
                displayName = "Ava Thompson",
                callState = CallState.ONGOING,
                timeSpentInCall = 127,
                availableAudioRoutes = listOf(
                    AudioRoute(name = "Speaker"),
                    AudioRoute(name = "Earpiece")
                )
            )
        )
    }
}
