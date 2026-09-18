package com.sosauce.cinnamon.features.phone.presentation.call

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
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
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.presentation.call.components.CallBottomBar
import com.sosauce.cinnamon.features.phone.presentation.call.components.IncomingBottomBar
import com.sosauce.nekobites.animations.bouncySpec
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallScreen(
    onCallAction: (CallAction) -> Unit,
    callUiState: CallingState
) {
    val isRinging = callUiState.callState == CallState.RINGING
    val isDialing = callUiState.callState == CallState.DIALING
    val cookie9Sided = MaterialShapes.Cookie9Sided.toShape()
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceContainerHighest = MaterialTheme.colorScheme.surfaceContainerHighest

    val shouldPulse = isRinging || isDialing

    val pulseScale1 = remember { Animatable(1f) }
    val pulseScale2 = remember { Animatable(1f) }

    LaunchedEffect(shouldPulse) {
        if (shouldPulse) {
            launch {
                pulseScale1.animateTo(
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                pulseScale2.animateTo(
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        } else {
            // Smoothly animate from current scale to 1f when pulsating should stop cuz else it's abrupt
            launch {
                pulseScale1.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
            }
            launch {
                pulseScale2.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Scaffold(
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
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = callUiState.poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .cloudy(15)
                    .fillMaxSize(),
                colorFilter = ColorFilter.tint(
                    color = Color.Black.copy(0.2f),
                    blendMode = BlendMode.Darken
                )
            )
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
            // Avatar vignette
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
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        DefaultContactIcon(
                            firstLetter = callUiState.displayName.firstOrNull(),
                            size = 184.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialShapes.Cookie9Sided.toShape(),
                            contactPfp = callUiState.photo,
                            modifier = Modifier
                                .drawWithCache {
                                    val path = cookie9Sided.createOutline(size, layoutDirection, this)
                                        .let { outline ->
                                            Path().apply {
                                                when (outline) {
                                                    is Outline.Generic -> addPath(outline.path)
                                                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                                                    is Outline.Rectangle -> addRect(outline.rect)
                                                }
                                            }
                                        }

                                    onDrawBehind {
                                        withTransform(
                                            {
                                                scale(
                                                    scaleX = 1.1f,
                                                    scaleY = 1.1f,
                                                    pivot = center
                                                )
                                            }
                                        ) {
                                            drawPath(
                                                path = path,
                                                color = surfaceContainerHighest.copy(alpha = 0.55f)
                                            )
                                        }
                                        withTransform(
                                            {
                                                scale(
                                                    scaleX = pulseScale2.value,
                                                    scaleY = pulseScale2.value,
                                                    pivot = center
                                                )
                                            }
                                        ) {
                                            drawPath(
                                                path = path,
                                                color = primaryContainer.copy(alpha = 0.09f)
                                            )
                                        }

                                        withTransform(
                                            {
                                                scale(
                                                    scaleX = pulseScale1.value,
                                                    scaleY = pulseScale1.value,
                                                    pivot = center
                                                )
                                            }
                                        ) {
                                            drawPath(
                                                path = path,
                                                color = primaryContainer.copy(alpha = 0.18f)
                                            )
                                        }
                                    }
                                }
                        )
                        // Status dot

                        val statusDotColor = if (callUiState.isHolding) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiaryContainer
                        androidx.compose.animation.AnimatedVisibility(
                            visible = callUiState.isHolding || callUiState.isMuted,
                            enter = scaleIn(bouncySpec()) + fadeIn(),
                            exit = scaleOut(bouncySpec()) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = statusDotColor,
                                modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (callUiState.isHolding) R.drawable.pause_filled else R.drawable.mic_off
                                    ),
                                    contentDescription = null,
                                    tint = contentColorFor(statusDotColor),
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }

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
                        AnimatedVisibility(
                            visible = callUiState.displayName.isNotBlank() && callUiState.number.isNotBlank() && callUiState.displayName != callUiState.number,
                            enter = scaleIn(),
                            exit = scaleOut()
                        ) {
                            Text(
                                text = callUiState.number,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        // EStatus chip
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
                                DateUtils.formatElapsedTime(callUiState.timeSpentInCall)
                            )
                        }

                        val chipIcon: Int?
                        val chipLabel: String

                        val chipColor by animateColorAsState(
                            targetValue = when (callUiState.callState) {
                                CallState.RINGING -> {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                                CallState.DIALING -> {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                                CallState.ENDED -> {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                                CallState.ONGOING -> {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            }
                        )

                        when (callUiState.callState) {
                            CallState.RINGING -> {
                                chipIcon = R.drawable.sim_card
                                chipLabel = secondaryText.text
                            }
                            CallState.DIALING -> {
                                chipIcon = R.drawable.phone
                                chipLabel = secondaryText.text
                            }
                            CallState.ENDED -> {
                                chipIcon = null
                                chipLabel = secondaryText.text
                            }
                            CallState.ONGOING -> {
                                chipIcon = R.drawable.timer
                                chipLabel = secondaryText.text
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (callUiState.callState == CallState.RINGING) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary,
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.sim_card_filled),
                                            contentDescription = null,
                                            tint = contentColorFor(MaterialTheme.colorScheme.primary,),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = secondaryText,
                                            style = MaterialTheme.typography.labelLargeEmphasized.copy(
                                                color = contentColorFor(MaterialTheme.colorScheme.primary,)
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
                                        disabledContainerColor = chipColor,
                                        disabledLabelColor = contentColorFor(chipColor),
                                        disabledLeadingIconContentColor = contentColorFor(chipColor)
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(50)
                                )
                            }
                            // prolly don't need it because status chip is enough
                            // On-hold expressive banner chip
//                            AnimatedVisibility(
//                                visible = callUiState.isHolding,
//                                enter = scaleIn() + fadeIn(),
//                                exit = scaleOut() + fadeOut()
//                            ) {
//                                Surface(
//                                    shape = RoundedCornerShape(50),
//                                    color = MaterialTheme.colorScheme.error,
//                                    tonalElevation = 2.dp,
//                                    modifier = Modifier.padding(top = 2.dp)
//                                ) {
//                                    Row(
//                                        verticalAlignment = Alignment.CenterVertically,
//                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
//                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
//                                    ) {
//                                        Icon(
//                                            painter = painterResource(R.drawable.pause_filled),
//                                            contentDescription = null,
//                                            tint = MaterialTheme.colorScheme.onError,
//                                            modifier = Modifier.size(14.dp)
//                                        )
//                                        Text(
//                                            text = stringResource(R.string.on_hold),
//                                            style = MaterialTheme.typography.labelMediumEmphasized.copy(
//                                                color = MaterialTheme.colorScheme.onError
//                                            )
//                                        )
//                                    }
//                                }
//                            }
                        }

                    }
                }
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
