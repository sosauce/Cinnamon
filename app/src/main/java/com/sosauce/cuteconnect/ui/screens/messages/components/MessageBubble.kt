@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.messages.components

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.provider.Telephony
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.data.actions.CommonAction
import com.sosauce.cuteconnect.domain.model.CuteContact
import com.sosauce.cuteconnect.domain.model.CuteMessage
import com.sosauce.cuteconnect.ui.shared_components.AnimatedSlider
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.utils.ImageUtils
import com.sosauce.cuteconnect.utils.getMMSSize
import com.sosauce.cuteconnect.utils.isEmoji
import com.sosauce.cuteconnect.utils.isLink
import com.sosauce.cuteconnect.utils.rememberInteractionSource
import com.sosauce.cuteconnect.utils.thenIf
import com.sosauce.cuteconnect.utils.toReadableDuration
import com.sosauce.cuteconnect.utils.toReadableTime
import java.io.File
import kotlin.time.DurationUnit

@UnstableApi
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    cuteMessage: CuteMessage,
    allMedias: List<Uri>,
    onAddMessageToSelected: () -> Unit = {},
    isSelected: Boolean = false,
    isInSelectMode: Boolean = false,
    sandwichPosition: SandwichPosition = SandwichPosition.SOLO,
    onHandleCommonAction: (CommonAction) -> Unit,
) {

    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val bubbleColor = when {
        cuteMessage.body.isEmoji() -> Color.Transparent
        cuteMessage.type == Telephony.Sms.MESSAGE_TYPE_SENT -> MaterialTheme.colorScheme.primaryFixedDim
        else -> MaterialTheme.colorScheme.tertiaryFixedDim
    }
    var isTimestampVisible by remember { mutableStateOf(false) }
    val alignment = remember {
        if (cuteMessage.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
            Alignment.End
        } else Alignment.Start
    }

    LaunchedEffect(cuteMessage.id) {
        if (!cuteMessage.read) {
            onHandleCommonAction(CommonAction.MarkMessageAsRead(cuteMessage.id))
        }
    }



    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isInSelectMode) {
                        onAddMessageToSelected()
                    } else {
                        isTimestampVisible = !isTimestampVisible
                    }
                },
                onLongClick = onAddMessageToSelected
            )
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
    ) {

        cuteMessage.attachment?.attachmentDetails?.let {

            it.fastForEachIndexed { index, details ->
            println("Attachment: ${context.contentResolver.getType(details.uri)}")

                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.contentColorFor(bubbleColor)) {
                    // TODO: have separate components for each
                    if (context.contentResolver.getType(details.uri)?.startsWith("image/") == true) {
                        AsyncImage(
                            model = ImageUtils.imageRequester(details.uri, context, true),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(
                                    vertical = 2.dp,
                                    horizontal = 10.dp,
                                )
                                .widthIn(max = configuration.screenWidthDp.dp * 0.7f)
                                .align(alignment)
                        )
                    } else if (context.contentResolver.getType(details.uri)?.startsWith("audio/") == true) {
                        // TODO Rewrite with Media3
                        var mediaPlayer: MediaPlayer? = remember { null }
                        var isPlaying by remember { mutableStateOf(false) }
                        val handler = Handler(context.mainLooper)
                        val checkPlayback = object : Runnable {
                            override fun run() {
                                isPlaying = mediaPlayer?.isPlaying == true
                                handler.postDelayed(this, 500)
                            }
                        }



                        DisposableEffect(Unit) {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(context, details.uri)
                                prepareAsync()
                                handler.post(checkPlayback)
                            }

                            onDispose {
                                if (!mediaPlayer.isPlaying) {
                                    mediaPlayer.stop()
                                    mediaPlayer.release()
                                    handler.removeCallbacks(checkPlayback)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = if (sandwichPosition == SandwichPosition.SOLO) 10.dp else 1.dp
                                )
                                .widthIn(max = configuration.screenWidthDp.dp * 0.8f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(bubbleColor)
                                .align(alignment)
//                            .onVisibilityChanged {
//                                if (!mediaPlayer.isPlaying) {
//                                    mediaPlayer.stop()
//                                    mediaPlayer.release()
//                                }
//                            }

                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp)
                            ) {
                                AnimatedSlider(
                                    value = 1f,
                                    onValueChanged = {},
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f)
                                )

                                PlayPauseButton(
                                    isPlaying = isPlaying,
                                    onClick = {
                                        if (mediaPlayer?.isPlaying == true) {
                                            mediaPlayer.pause()
                                        } else mediaPlayer?.start()
                                    }
                                )
                            }
                        }
                    } else if (context.contentResolver.getType(details.uri)?.startsWith("video/") == true) {

                        val player = remember {
                            ExoPlayer
                                .Builder(context)
                                .build()
                                .apply {
                                    setMediaItem(MediaItem.fromUri(details.uri))
                                }
                        }

                        Box(
                            modifier = Modifier.align(alignment)
                        ) {
                            PlayerSurface(
                                player = player,
                                modifier = Modifier
                                    .size(400.dp)
                                    //.widthIn(max = configuration.screenWidthDp.dp * 0.7f)
                                    .clip(RoundedCornerShape(24.dp)),
                                surfaceType = SURFACE_TYPE_TEXTURE_VIEW
                            )

                            IconButton(
                                onClick = { player.play() }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null
                                )
                            }
                        }
                    } else {
                        val fileSize = remember { context.getMMSSize(cuteMessage.attachment.attachmentDetails[index].uri) }


                        Box(
                            modifier = Modifier
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = if (sandwichPosition == SandwichPosition.SOLO) 10.dp else 1.dp
                                )
                                .widthIn(max = configuration.screenWidthDp.dp * 0.8f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(bubbleColor)
                                .align(alignment)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.file),
                                    contentDescription = null
                                )
                                Column {
                                    CuteText(
                                        text = cuteMessage.attachment.attachmentDetails[index].filename,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    CuteText(
                                        text = Formatter.formatFileSize(
                                            context,
                                            fileSize
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

            }

        }
        if (if (!cuteMessage.isMms) cuteMessage.body.isNotEmpty() else cuteMessage.attachment?.body?.isNotEmpty() == true) {
            Box(
                modifier = Modifier
                    .padding(
                        horizontal = 10.dp,
                        vertical = if (sandwichPosition == SandwichPosition.SOLO) 10.dp else 1.dp
                    )
                    .widthIn(max = configuration.screenWidthDp.dp * 0.8f)
                    .clip(
                        BubbleShape(
                            sandwichPosition = sandwichPosition,
                            messageType = cuteMessage.type
                        )
                    )

                    .background(bubbleColor)
                    .align(alignment)
                    .thenIf(cuteMessage.body.isLink()) {
                        Modifier.clickable {
                            uriHandler.openUri(cuteMessage.body)
                        }
                    }

            ) {
                CuteText(
                    text = if (!cuteMessage.isMms) cuteMessage.body else cuteMessage.attachment?.body ?: "",
                    modifier = Modifier
                        .padding(10.dp),
                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                        fontSize = if (cuteMessage.body.isEmoji()) 35.sp else TextUnit.Unspecified,
                        color = MaterialTheme.colorScheme.contentColorFor(bubbleColor),
                        textDecoration = if (cuteMessage.body.isLink()) TextDecoration.Underline else null,
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = isTimestampVisible && !isSelected,
            modifier = Modifier
                .align(alignment)
                .padding(horizontal = 10.dp)
        ) {
            CuteText(
                text = cuteMessage.date.toReadableDuration(DurationUnit.MILLISECONDS),
                color = MaterialTheme.colorScheme.onBackground.copy(0.85f),
                fontSize = 13.sp,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(2.dp),
                style = TextStyle()
            )
        }
    }
}

enum class SandwichPosition {
    SOLO, TOP, MIDDLE, BOTTOM
}

fun BubbleShape(
    sandwichPosition: SandwichPosition,
    messageType: Int
): Shape {

    return if (messageType == Telephony.Sms.MESSAGE_TYPE_SENT) {
        RoundedCornerShape(
            topStart = 24.dp,
            bottomStart = 24.dp,
            topEnd = when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.TOP -> 24.dp
                else -> 4.dp
            },
            bottomEnd = when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.BOTTOM -> 24.dp
                else -> 4.dp
            }
        )
    } else {
        RoundedCornerShape(
            topStart = when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.TOP -> 24.dp
                else -> 4.dp
            },
            bottomStart = when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.BOTTOM -> 24.dp
                else -> 4.dp
            },
            topEnd = 24.dp,
            bottomEnd = 24.dp
        )
    }
}

@Composable
fun PlayPauseButton(
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    isPlaying: Boolean,
    onClick: () -> Unit
) {

    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f
    )



    IconButton(
        onClick = onClick,
        modifier = buttonModifier,
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}
