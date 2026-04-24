@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components.bubble

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.buttons.PlayPauseButton
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.shared_components.AnimatedSlider
import com.sosauce.cinnamon.presentation.shared_components.buttons.WavySlider
import kotlinx.coroutines.delay

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun AudioAttachment(
    audio: Uri,
    bubbleColor: Color
) {
    val context = LocalContext.current
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    val player = remember(context.applicationContext) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(MediaItem.fromUri(audio))
            prepare()
        }
    }


    LaunchedEffect(Unit) {
        while (true) {
            position = player.currentPosition.toFloat()
            duration = player.duration.coerceAtLeast(0).toFloat()
            delay(500)
        }
    }

    RetainedEffect(player) {
        onRetire {
            player.release()
        }
    }


    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bubbleColor)
    ) {
        Row(
            modifier = Modifier.padding(10.dp)
        ) {
            PlayPauseButton(player) {
                IconButton(
                    onClick = this::onClick,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    val icon = if (this.showPlay) {
                        R.drawable.play_filled
                    } else R.drawable.pause_filled

                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null
                    )
                }
            }

            var tempSlider by remember { mutableStateOf<Float?>(null) }
            WavySlider(
                value = position,
                valueRange = 0f..duration,
                onValueChangeFinished = { newValue ->
                    player.seekTo(newValue.toLong())
                    tempSlider = null
                },
                modifier = Modifier.weight(1f)
            )

        }
    }

}