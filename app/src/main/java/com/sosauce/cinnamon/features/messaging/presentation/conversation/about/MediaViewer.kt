@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.about

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.buttons.WavySlider
import com.sosauce.cinnamon.core.utils.ImageUtils
import com.sosauce.cinnamon.core.utils.toMediaTime
import com.sosauce.cinnamon.features.messaging.data.model.AttachmentType
import com.sosauce.cinnamon.features.messaging.data.model.CuteAttachment
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.nekobites.animations.AnimatedCounter
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MediaViewerDialog(
    medias: List<CuteAttachment.AttachmentDetails>,
    initialIndex: Int,
    onDismissRequest: () -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { medias.size }
    val thumbnailsState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentMedia = medias.getOrNull(pagerState.currentPage)

    LaunchedEffect(pagerState.currentPage) {
        thumbnailsState.animateScrollToItem((pagerState.currentPage - 2).coerceAtLeast(0))
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                ) {
                    ViewerButton(
                        icon = R.drawable.close,
                        contentDescription = stringResource(R.string.close),
                        onClick = onDismissRequest
                    )

                    Row(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                    ) {

                        val style = MaterialTheme.typography.titleMediumEmphasized.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        CompositionLocalProvider(LocalTextStyle provides style) {
                            AnimatedCounter(
                                count = pagerState.currentPage + 1,
                                textStyle = style
                            )
                            Text("/")
                            Text(
                                text = medias.size.toString()
                            )
                        }
                    }

                    val canSave = currentMedia?.attachmentType == AttachmentType.IMAGE
                    Box(
                        modifier = Modifier.size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = canSave,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ViewerButton(
                                icon = R.drawable.download,
                                contentDescription = stringResource(R.string.save),
                                onClick = {
                                    currentMedia?.let {
                                        onHandleConversationActions(ConversationActions.DownloadMmsImage(it.uri))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                LazyRow(
                    state = thumbnailsState,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 15.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    itemsIndexed(
                        items = medias,
                        key = { _, media -> media.id }
                    ) { index, media ->
                        val isSelected = index == pagerState.currentPage
                        val size by animateDpAsState(if (isSelected) 64.dp else 52.dp)
                        MediaThumbnail(
                            media = media,
                            size = size,
                            isSelected = isSelected,
                            dimmed = !isSelected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }
            }
        ) { _ ->

            currentMedia?.let { media ->
                AsyncImage(
                    model = media.previewModel(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .cloudy(100)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 15.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val media = medias[page]
                    if (media.attachmentType == AttachmentType.VIDEO) {
                        VideoPage(
                            video = media.uri,
                            isActive = page == pagerState.settledPage
                        )
                    } else {
                        ImagePage(image = media.uri)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePage(image: Uri) {
    val zoomState = rememberZoomState()
    var aspectRatio by remember { mutableStateOf<Float?>(null) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                zoomState.setContentSize(size)
                aspectRatio = size.aspectRatioOrNull()
            },
            modifier = Modifier
                .aspectRatioOrFill(aspectRatio)
                .zoomable(zoomState)
                .clip(PageShape)
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun VideoPage(
    video: Uri,
    isActive: Boolean
) {
    val context = LocalContext.current

    if (!isActive) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            var aspectRatio by remember { mutableStateOf<Float?>(null) }
            AsyncImage(
                model = remember(video) { ImageUtils.videoFrameRequester(video, context) },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { state -> aspectRatio = state.painter.intrinsicSize.aspectRatioOrNull() },
                modifier = Modifier
                    .aspectRatioOrFill(aspectRatio)
                    .clip(PageShape)
            )
            PlayBadge()
        }
        return
    }

    val exoPlayer = remember(video) {
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
    var videoAspectRatio by remember { mutableStateOf<Float?>(null) }
    var tempSliderValue by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
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

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ContentFrame(
                player = exoPlayer,
                modifier = Modifier
                    .aspectRatioOrFill(videoAspectRatio)
                    .clip(PageShape)
            )
        }
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
                        tempSliderValue?.let { exoPlayer.seekTo(it.toLong()) }
                        tempSliderValue = null
                    },
                    isPlaying = isPlaying
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
fun MediaThumbnail(
    media: CuteAttachment.AttachmentDetails,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    isSelected: Boolean = false,
    dimmed: Boolean = false,
    onClick: () -> Unit
) {
    val dimAlpha by animateFloatAsState(if (dimmed) 0.5f else 0f)
    val shape = RoundedCornerShape(12.dp)
    val description = stringResource(
        if (media.attachmentType == AttachmentType.VIDEO) R.string.video else R.string.image
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape
            )
            .clickable(onClickLabel = description, onClick = onClick)
    ) {
        AsyncImage(
            model = media.previewModel(),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (media.attachmentType == AttachmentType.VIDEO) {
            PlayBadge(small = size != null)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
        )
    }
}

@Composable
private fun PlayBadge(small: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(
                if (small) 24.dp
                else IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform).width
            )
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialShapes.Cookie9Sided.toShape()
            )
    ) {
        Icon(
            painter = painterResource(R.drawable.play_filled),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = if (small) Modifier.size(14.dp) else Modifier
        )
    }
}

@Composable
private fun ViewerButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
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
            painter = painterResource(icon),
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun CuteAttachment.AttachmentDetails.previewModel(): Any {
    val context = LocalContext.current
    return if (attachmentType == AttachmentType.VIDEO) {
        remember(uri) { ImageUtils.videoFrameRequester(uri, context) }
    } else {
        uri
    }
}

private val PageShape = RoundedCornerShape(24.dp)

private fun Size.aspectRatioOrNull(): Float? =
    if (isSpecified && width > 0f && height > 0f) width / height else null

private fun Modifier.aspectRatioOrFill(aspectRatio: Float?): Modifier =
    if (aspectRatio != null) aspectRatio(aspectRatio) else fillMaxSize()
