@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.ui.compose.buttons.PlayPauseButton
import com.sosauce.cinnamon.core.ui.components.buttons.WavySlider
import com.sosauce.cinnamon.core.utils.rememberInteractionSource
import com.sosauce.cinnamon.core.utils.toMediaTime
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.animations.rememberAnimatedShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun AudioAttachment(
    audio: Uri,
    bubbleColor: Color
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tempSliderValue by remember { mutableStateOf<Float?>(null) }
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableStateOf<Float?>(null) }
    var isPlayerPlaying by remember { mutableStateOf(false) }
    val player = retain {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(MediaItem.fromUri(audio))
            prepare()
        }
    }


    RetainedEffect(player) {

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                isPlayerPlaying = isPlaying
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                scope.launch {
                    ensureActive()
                    while (player.isPlaying) {
                        position = player.currentPosition.toFloat()
                        delay(1.seconds)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)

                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    val rawDuration = player.duration
                    if (rawDuration != C.TIME_UNSET && rawDuration > 0) {
                        duration = rawDuration.toFloat()
                    }
                }
            }
        }

        player.addListener(listener)

        onRetire {
            player.removeListener(listener)
            player.release()
        }
    }


    val contentColor = contentColorFor(bubbleColor)
    val playInteractionSource = rememberInteractionSource()
    val isPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = bouncySpec()
    )
    val playShape = rememberAnimatedShape(
        condition = isPlayerPlaying,
        shapeA = MaterialShapes.Circle,
        shapeB = MaterialShapes.Cookie9Sided
    )
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlayerPlaying) {
        while (isPlayerPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(10000, easing = LinearEasing)
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .widthIn(min = 270.dp)
            .background(bubbleColor, RoundedCornerShape(24.dp))
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        PlayPauseButton(player) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = this::onClick
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { rotationZ = rotation.value }
                        .background(contentColor, playShape)
                )
                CompositionLocalProvider(LocalContentColor provides bubbleColor) {
                    AnimatedDrawable(
                        drawable = AnimatedDrawableFile.PLAY,
                        atEnd = !showPlay,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // recomposes the slider once the duration is set so it can have the proper value range
            key(duration) {
                WavySlider(
                    value = tempSliderValue ?: position,
                    valueRange = 0f..(duration ?: 0f),
                    onValueChange = { tempSliderValue = it },
                    onValueChangeFinished = {
                        tempSliderValue?.let {
                            player.seekTo(it.toLong())
                            position = it
                        }
                        tempSliderValue = null
                    },
                    colors = SliderDefaults.colors(thumbColor = contentColor),
                    indicatorColor = contentColor,
                    trackColor = contentColor.copy(alpha = 0.24f),
                    isPlaying = isPlayerPlaying
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                AnimatedTime(
                    text = (tempSliderValue ?: position).toLong().toMediaTime(),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = contentColor
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = (duration ?: 0f).toLong().toMediaTime(),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier
                        .background(contentColor.copy(alpha = 0.14f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }

}

@Composable
private fun AnimatedTime(
    text: String,
    style: TextStyle,
    color: Color
) {
    Row {
        text.forEachIndexed { index, char ->
            key(index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically(bouncySpec()) { it }) togetherWith
                                (slideOutVertically(bouncySpec()) { -it })
                    }
                ) { c ->
                    Text(
                        text = c.toString(),
                        style = style,
                        color = color
                    )
                }
            }
        }
    }
}
