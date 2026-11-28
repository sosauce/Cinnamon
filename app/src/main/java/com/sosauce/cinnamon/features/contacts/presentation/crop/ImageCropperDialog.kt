@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.crop

import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.components.LoadingBox
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private const val MAX_ZOOM = 5f

@Composable
fun ImageCropperDialog(
    uri: Uri,
    mode: CropMode,
    onDismissRequest: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var source by remember { mutableStateOf<CropSource?>(null) }
    var ops by remember { mutableStateOf(emptyList<CropOp>()) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var center by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var isGesturing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    var contentPadding by remember { mutableStateOf(PaddingValues()) }

    LaunchedEffect(uri) {
        source = loadCropSource(context, uri)
        if (source == null) {
            Toast.makeText(context, resources.getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            onDismissRequest()
        }
    }

    val image = remember(source, ops) { source?.render(ops)?.asImageBitmap() }
    val imageSize = image?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: Size.Zero
    val frame = remember(areaSize, contentPadding, mode) {
        with(density) {
            val safeArea = Rect(
                0f,
                contentPadding.calculateTopPadding().toPx(),
                areaSize.width.toFloat(),
                areaSize.height - contentPadding.calculateBottomPadding().toPx()
            )
            fitFrame(safeArea, mode.aspectRatio, 24.dp.toPx())
        }
    }
    val framePath = remember(frame, mode) {
        Path().apply {
            when (mode) {
                CropMode.ProfilePicture -> addOval(frame)
                CropMode.Poster -> addRoundRect(RoundRect(frame, CornerRadius(with(density) { 28.dp.toPx() })))
            }
        }
    }
    val isReady = image != null && frame.width > 0f
    val half = if (isReady) halfExtent(frame.size, imageSize, zoom) else Size(0.5f, 0.5f)
    val visibleCenter = center.clampTo(half)
    val gridAlpha by animateFloatAsState(if (isGesturing) 1f else 0f)

    fun transform(centroid: Offset, pan: Offset, zoomChange: Float) {
        val oldScale = coverScale(frame.size, imageSize) * zoom
        val newZoom = (zoom * zoomChange).coerceIn(1f, MAX_ZOOM)
        val newScale = coverScale(frame.size, imageSize) * newZoom
        val fromCenter = centroid - frame.center
        val anchor = center.clampTo(halfExtent(frame.size, imageSize, zoom)) + fromCenter.toNormalized(imageSize, oldScale)
        val newCenter = anchor - (fromCenter + pan).toNormalized(imageSize, newScale)
        zoom = newZoom
        center = newCenter.clampTo(halfExtent(frame.size, imageSize, newZoom))
    }

    fun applyOp(op: CropOp) {
        val point = floatArrayOf(visibleCenter.x, visibleCenter.y).also { listOf(op).toMatrix().mapPoints(it) }
        ops = ops + op
        center = Offset(point[0], point[1])
    }

    fun reset() {
        ops = emptyList()
        zoom = 1f
        center = Offset(0.5f, 0.5f)
    }

    fun save() {
        val currentSource = source ?: return
        val crop = RectF(
            visibleCenter.x - half.width,
            visibleCenter.y - half.height,
            visibleCenter.x + half.width,
            visibleCenter.y + half.height
        )
        isExporting = true
        scope.launch {
            val cropped = exportCrop(context, uri, currentSource, ops, crop)
            isExporting = false
            if (cropped != null) {
                onCropped(cropped)
            } else {
                Toast.makeText(context, resources.getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
            }
        }
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
            bottomBar = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 15.dp)
                ) {
                    CropToolbar(
                        onApplyOp = ::applyOp,
                        onReset = ::reset
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CuteNavigationButtonSurface(onNavigateUp = onDismissRequest)
                        AnimatedFab(
                            onClick = ::save,
                            icon = R.drawable.check,
                            enabled = isReady && !isExporting
                        )
                    }
                }
            }
        ) { paddingValues ->
            LaunchedEffect(paddingValues) { contentPadding = paddingValues }
            LoadingBox(isLoading = image == null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { areaSize = it }
                        .pointerInput(image, frame) {
                            if (!isReady) return@pointerInput
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                isGesturing = true
                                do {
                                    val event = awaitPointerEvent()
                                    val zoomChange = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    if (zoomChange != 1f || pan != Offset.Zero) {
                                        transform(event.calculateCentroid(), pan, zoomChange)
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                                isGesturing = false
                            }
                        }
                ) {
                    if (image == null || !isReady) return@Canvas
                    val imageScale = coverScale(frame.size, imageSize) * zoom
                    val topLeft = frame.center - Offset(
                        visibleCenter.x * imageSize.width * imageScale,
                        visibleCenter.y * imageSize.height * imageScale
                    )

                    translate(topLeft.x, topLeft.y) {
                        scale(imageScale, pivot = Offset.Zero) {
                            drawImage(image, filterQuality = FilterQuality.High)
                        }
                    }

                    clipPath(framePath, ClipOp.Difference) {
                        drawRect(colors.scrim.copy(alpha = 0.6f))
                    }

                    if (gridAlpha > 0f) {
                        clipPath(framePath) {
                            val gridColor = colors.primary.copy(alpha = 0.6f * gridAlpha)
                            for (i in 1..2) {
                                val x = frame.left + frame.width * i / 3
                                val y = frame.top + frame.height * i / 3
                                drawLine(gridColor, Offset(x, frame.top), Offset(x, frame.bottom), 1.dp.toPx())
                                drawLine(gridColor, Offset(frame.left, y), Offset(frame.right, y), 1.dp.toPx())
                            }
                        }
                    }

                    drawPath(framePath, colors.primary, style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun CropToolbar(
    onApplyOp: (CropOp) -> Unit,
    onReset: () -> Unit
) {
    val items = listOf(
        CropToolbarItem(R.drawable.rotate_left, R.string.rotate_left) { onApplyOp(CropOp.RotateLeft) },
        CropToolbarItem(R.drawable.rotate_right, R.string.rotate_right) { onApplyOp(CropOp.RotateRight) },
        CropToolbarItem(R.drawable.flip, R.string.flip_horizontal) { onApplyOp(CropOp.FlipHorizontal) },
        CropToolbarItem(R.drawable.flip, R.string.flip_vertical, iconRotation = 90f) { onApplyOp(CropOp.FlipVertical) },
        CropToolbarItem(R.drawable.restart_alt, R.string.reset, onClick = onReset)
    )
    val interactionSources = List(items.size) { remember { MutableInteractionSource() } }
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
    )

    ButtonGroup(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        overflowIndicator = {}
    ) {
        items.forEachIndexed { index, item ->
            customItem(
                buttonGroupContent = {
                    Button(
                        onClick = item.onClick,
                        interactionSource = interactionSources[index],
                        shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 4.dp, bottomEnd = 4.dp)
                            items.lastIndex -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 50.dp, bottomEnd = 50.dp)
                            else -> RoundedCornerShape(4.dp)
                        },
                        colors = buttonColors,
                        modifier = Modifier
                            .animateWidth(interactionSources[index])
                            .weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = stringResource(item.description),
                            modifier = Modifier.rotate(item.iconRotation)
                        )
                    }
                },
                menuContent = {}
            )
        }
    }
}

private data class CropToolbarItem(
    @DrawableRes val icon: Int,
    @StringRes val description: Int,
    val iconRotation: Float = 0f,
    val onClick: () -> Unit
)

private fun fitFrame(area: Rect, aspectRatio: Float, padding: Float): Rect {
    val width = min(area.width - padding * 2, (area.height - padding * 2) * aspectRatio)
    if (width <= 0f) return Rect.Zero
    val height = width / aspectRatio
    return Rect(Offset(area.center.x - width / 2, area.center.y - height / 2), Size(width, height))
}

private fun coverScale(frame: Size, image: Size) = max(frame.width / image.width, frame.height / image.height)

private fun halfExtent(frame: Size, image: Size, zoom: Float): Size {
    val scale = coverScale(frame, image) * zoom
    return Size(
        (frame.width / (2 * image.width * scale)).coerceAtMost(0.5f),
        (frame.height / (2 * image.height * scale)).coerceAtMost(0.5f)
    )
}

private fun Offset.toNormalized(image: Size, scale: Float) =
    Offset(x / (image.width * scale), y / (image.height * scale))

private fun Offset.clampTo(half: Size) = Offset(
    x.coerceIn(half.width, 1f - half.width),
    y.coerceIn(half.height, 1f - half.height)
)
