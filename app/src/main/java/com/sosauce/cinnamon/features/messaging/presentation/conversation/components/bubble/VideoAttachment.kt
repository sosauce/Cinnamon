@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.buttons.WavySlider
import com.sosauce.cinnamon.core.utils.ImageUtils
import com.sosauce.cinnamon.core.utils.rememberInteractionSource
import com.sosauce.cinnamon.core.utils.toMediaTime
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.animations.rememberAnimatedShape
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun VideoAttachment(
    video: Uri,
    modifier: Modifier = Modifier
) {


    val context = LocalContext.current
    val request = remember(video) { ImageUtils.videoFrameRequester(video, context) }

    var showFullscreen by remember { mutableStateOf(false) }

    var tempSliderValue by remember { mutableStateOf<Float?>(null) }


    if (showFullscreen) {

        val exoPlayer = retain {
            ExoPlayer.Builder(context.applicationContext)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    setMediaItem(MediaItem.fromUri(video))
                    prepare()
                }
        }
        var isPlaying by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableLongStateOf(0) }
        var duration by remember { mutableLongStateOf(0) }


        RetainedEffect(exoPlayer) {

            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying1: Boolean) {
                    super.onIsPlayingChanged(isPlaying1)
                    isPlaying = isPlaying1
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {
                        duration = exoPlayer.duration.coerceAtLeast(0)
                    }
                }
            }

            exoPlayer.addListener(listener)

            onRetire {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0)
                delay(500.milliseconds)
            }
        }

        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { showFullscreen = false }
            ) {
                Box(
                    modifier = Modifier.cloudy(100)
                ) {
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(15.dp)
                ) {
                    FilledIconButton(
                        onClick = { showFullscreen = false },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(
                            IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_down),
                            contentDescription = null
                        )
                    }

                    ContentFrame(
                        player = exoPlayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(MaterialTheme.shapes.extraLarge)
                    )

                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            WavySlider(
                                value = tempSliderValue ?: currentPosition.toFloat(),
                                valueRange = 0f..duration.toFloat(),
                                onValueChange = { tempSliderValue = it },
                                onValueChangeFinished = {
                                    tempSliderValue?.let {
                                        exoPlayer.seekTo(it.toLong())
                                    }
                                    tempSliderValue = null
                                },
                                isPlaying = isPlaying
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (tempSliderValue?.toLong() ?: currentPosition).toMediaTime(),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier.size(
                                        IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)
                                    )
                                ) {
                                    AnimatedDrawable(
                                        drawable = AnimatedDrawableFile.PLAY,
                                        atEnd = isPlaying
                                    )
                                }
                                Text(
                                    text = duration.toMediaTime(),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = bouncySpec()
    )
    val badgeShape = rememberAnimatedShape(
        condition = isPressed,
        shapeA = MaterialShapes.Cookie9Sided,
        shapeB = MaterialShapes.Circle
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { showFullscreen = true }
    ) {
        AsyncImage(
            model = request,
            contentDescription = null
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform))
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = badgeShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.play_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
//        Row(
//            modifier = Modifier
//                .padding(5.dp)
//                .background(
//                    color = MaterialTheme.colorScheme.surface,
//                    shape = RoundedCornerShape(24.dp)
//                )
//                .align(Alignment.BottomStart)
//
//        ) {
//            Text(
//                text = "00:05",
//                modifier = Modifier.padding(3.dp),
//                color = MaterialTheme.colorScheme.onSurface
//            )
//        }
    }


//
//    val player = remember {
//        ExoPlayer
//            .Builder(context.applicationContext)
//            .build()
//            .apply {
//                addMediaItem(MediaItem.fromUri(video))
//                prepare()
//            }
//    }
//
//    RetainedEffect(player) {
//        onRetire {
//            player.release()
//        }
//    }
//
//    Box(
//        modifier = modifier.size(800.dp)
//    ) {
//        ContentFrame(
//            player = player,
//            modifier = Modifier.fillMaxSize()
//        )
//    }
}
