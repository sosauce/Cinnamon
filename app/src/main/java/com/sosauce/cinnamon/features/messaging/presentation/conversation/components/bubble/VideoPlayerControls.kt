@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sosauce.cinnamon.core.utils.toMediaTime
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile

/**
 * Player that starts playing [video] right away and loops it, the caller has to release it
 */
@SuppressLint("UnsafeOptInUsageError")
fun createLoopingVideoPlayer(context: Context, video: Uri): ExoPlayer {
    return ExoPlayer.Builder(context.applicationContext)
        .build()
        .apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(video))
            prepare()
        }
}

/**
 * Current position, play/pause button and duration of a video, shown under its slider
 */
@Composable
fun VideoPlaybackRow(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = position.toMediaTime(),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        FilledIconButton(
            onClick = onPlayPause,
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
