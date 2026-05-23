@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components.bubble

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.sosauce.cinnamon.presentation.shared_components.buttons.WavySlider
import com.sosauce.cinnamon.utils.ImageUtils
import kotlinx.coroutines.delay

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
                delay(500)
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
                    .clickable { showFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.cloudy(250)
                ) {
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        modifier = modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                IconButton(
                    onClick = { showFullscreen = false },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                    ),
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier
                        .padding(horizontal = 15.dp)
                        .statusBarsPadding()
                        .align(Alignment.TopStart)
                        .size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.keyboard_down),
                        contentDescription = null
                    )
                }

                ContentFrame(
                    player = exoPlayer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FilledIconButton(
                        onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        val icon =
                            if (isPlaying) R.drawable.pause_filled else R.drawable.play_filled
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        WavySlider(
                            modifier = Modifier
                                .padding(10.dp),
                            value = animateFloatAsState(
                                targetValue = tempSliderValue ?: currentPosition.toFloat()
                            ).value,
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
                    }
                }

            }
        }
    }


    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { showFullscreen = true }
    ) {
        AsyncImage(
            model = request,
            contentDescription = null
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialShapes.Circle.toShape()
                )
                .size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.play_filled),
                contentDescription = null,
                tint = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
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