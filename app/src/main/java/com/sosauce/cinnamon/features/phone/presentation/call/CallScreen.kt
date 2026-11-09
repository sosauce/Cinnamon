package com.sosauce.cinnamon.features.phone.presentation.call

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberIsLandscape
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.ui.barsContentTransform
import com.sosauce.cinnamon.core.ui.components.CuteAvatar
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
    val isLandscape = rememberIsLandscape()
    val shouldPulse = isRinging || isDialing
    val pulseScale1 = remember { Animatable(1f) }
    val pulseScale2 = remember { Animatable(1f) }

    LaunchedEffect(shouldPulse) {
        if (shouldPulse) {
            launch {
                pulseScale1.animateTo(
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                pulseScale2.animateTo(
                    targetValue = 1.22f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        } else {
            launch {
                pulseScale1.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            }
            launch {
                pulseScale2.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            }
        }
    }

    val bottomControls: @Composable () -> Unit = {
        AnimatedContent(
            targetState = isRinging,
            transitionSpec = { barsContentTransform }
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

    Scaffold(
        bottomBar = { if (!isLandscape) bottomControls() }
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = callUiState.poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .cloudy(100)
                    .fillMaxSize(),
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    blendMode = BlendMode.Darken
                )
            )

            if (isLandscape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    CallerInfo(
                        callUiState = callUiState,
                        avatarSize = 120.dp,
                        pulseScale1 = { pulseScale1.value },
                        pulseScale2 = { pulseScale2.value },
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        bottomControls()
                    }
                }
            } else {
                CallerInfo(
                    callUiState = callUiState,
                    avatarSize = 184.dp,
                    pulseScale1 = { pulseScale1.value },
                    pulseScale2 = { pulseScale2.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CallerInfo(
    callUiState: CallingState,
    avatarSize: Dp,
    pulseScale1: () -> Float,
    pulseScale2: () -> Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition()
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(avatarSize * 1.3f)
        ) {
            Box(
                Modifier
                    .size(avatarSize * 1.2f)
                    .graphicsLayer {
                        scaleX = pulseScale2()
                        scaleY = pulseScale2()
                        rotationZ = -spin
                    }
                    .background(colors.tertiaryContainer.copy(alpha = 0.5f), MaterialShapes.Cookie9Sided.toShape())
            )
            Box(
                Modifier
                    .size(avatarSize * 1.12f)
                    .graphicsLayer {
                        scaleX = pulseScale1()
                        scaleY = pulseScale1()
                        rotationZ = spin
                    }
                    .background(colors.primaryContainer, MaterialShapes.Cookie12Sided.toShape())
            )
            Box {
                CuteAvatar(
                    name = callUiState.displayName,
                    photo = callUiState.photo,
                    size = avatarSize,
                    containerColor = colors.surfaceContainerHighest,
                    contentColor = colors.onSurface
                )
                val statusColor by animateColorAsState(
                    if (callUiState.isHolding) colors.error else colors.tertiary
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = callUiState.isHolding || callUiState.isMuted,
                    enter = scaleIn(bouncySpec()) + fadeIn(),
                    exit = scaleOut(bouncySpec()) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(statusColor, MaterialShapes.Sunny.toShape())
                    ) {
                        Icon(
                            painter = painterResource(
                                if (callUiState.isHolding) R.drawable.pause_filled else R.drawable.mic_off
                            ),
                            contentDescription = null,
                            tint = contentColorFor(statusColor),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = callUiState.displayName.ifBlank { callUiState.number.ifBlank { stringResource(R.string.unknown) } },
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )
        AnimatedVisibility(
            visible = callUiState.displayName.isNotBlank() && callUiState.number.isNotBlank() && callUiState.displayName != callUiState.number,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = callUiState.number,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = colors.secondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
        Spacer(Modifier.height(16.dp))
        CallStatusPill(callUiState)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CallStatusPill(callUiState: CallingState) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (callUiState.callState) {
            CallState.RINGING -> colors.primary
            CallState.DIALING -> colors.tertiaryContainer
            CallState.ENDED -> colors.surfaceContainerHighest
            CallState.ONGOING -> colors.primaryContainer
        }
    )
    val content = contentColorFor(container)
    val icon = when (callUiState.callState) {
        CallState.RINGING -> R.drawable.sim_card_filled
        CallState.DIALING -> R.drawable.phone
        CallState.ENDED -> R.drawable.hang_up_filled
        CallState.ONGOING -> R.drawable.timer
    }
    val label: AnnotatedString = when (callUiState.callState) {
        CallState.RINGING -> buildAnnotatedString {
            append(stringResource(R.string.via))
            append(" ")
            withStyle(SpanStyle(color = Color(callUiState.activeSim.color))) {
                append(callUiState.activeSim.name)
            }
        }
        CallState.DIALING -> AnnotatedString(stringResource(R.string.ringing))
        CallState.ENDED -> AnnotatedString(stringResource(R.string.call_ended))
        CallState.ONGOING -> AnnotatedString(DateUtils.formatElapsedTime(callUiState.timeSpentInCall))
    }

    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = { scaleIn(bouncySpec()) togetherWith scaleOut(bouncySpec()) }
            ) {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
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
