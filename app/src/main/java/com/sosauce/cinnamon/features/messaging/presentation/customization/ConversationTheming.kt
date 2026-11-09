@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.customization

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberAppTheme
import com.sosauce.cinnamon.core.datastore.rememberIsLandscape
import com.sosauce.cinnamon.core.datastore.rememberPaletteStyle
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.core.ui.components.buttons.WavySlider
import com.sosauce.cinnamon.core.utils.CuteTheme
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.core.utils.toPaletteStyle
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingActions
import com.sosauce.cinnamon.features.messaging.domain.MessageType
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.SandwichPosition
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.TextBubbleContent
import com.sosauce.cinnamon.settings.components.SettingsGroup
import com.sosauce.cinnamon.settings.components.SettingsItem
import com.sosauce.cinnamon.settings.components.SettingsRowContent
import com.sosauce.nekobites.components.ColorPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ConversationTheming(
    state: ThemingState,
    onHandleConversationSettingsActions: (ConversationSettingActions) -> Unit,
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showColorPicker by remember { mutableStateOf(false) }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch(Dispatchers.IO) {
                state.settings.wallpaper?.path?.let {
                    File(it).delete()
                }

                val file = File(
                    context.filesDir,
                    "wallpaper_${state.settings.threadId}_${System.currentTimeMillis()}.jpg"
                )

                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                onHandleConversationSettingsActions(
                    ConversationSettingActions.UpsertConversationSettings(
                        state.settings.copy(
                            wallpaper = file.toUri()
                        )
                    )
                )
            }
        }

    val removeWallpaper: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            state.settings.wallpaper?.path?.let {
                File(context.filesDir, it).delete()
            }
            onHandleConversationSettingsActions(
                ConversationSettingActions.UpsertConversationSettings(
                    state.settings.copy(wallpaper = null)
                )
            )
        }
    }
    val pickWallpaper: () -> Unit = {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    var blur by remember(state.settings.wallpaperBlurIntensity) { mutableFloatStateOf(state.settings.wallpaperBlurIntensity.toFloat()) }
    val isLandscape = rememberIsLandscape()

    if (showColorPicker) {
        ColorPickerDialog(
            onDismissRequest = { showColorPicker = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.colorize),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.color_picker)) },
            dismissButton = {
                TextButton(
                    onClick = { showColorPicker = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = { newColor ->
                TextButton(
                    onClick = {
                        onHandleConversationSettingsActions(
                            ConversationSettingActions.UpsertConversationSettings(
                                state.settings.copy(
                                    color = newColor
                                )
                            )
                        )
                        showColorPicker = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        )
    }

    val preview: @Composable (Modifier) -> Unit = { modifier ->
        ChatPreview(
            wallpaper = state.settings.wallpaper,
            blur = blur.roundToInt(),
            seedColor = state.settings.color,
            onClick = pickWallpaper,
            modifier = modifier
        )
    }

    val settings: @Composable ColumnScope.() -> Unit = {
        WallpaperActions(
            hasWallpaper = state.settings.wallpaper != null,
            onPick = pickWallpaper,
            onRemove = removeWallpaper
        )
        AnimatedVisibility(
            visible = state.settings.wallpaper != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsGroup(title = stringResource(R.string.wallpaper)) {
                SettingsItem {
                    SettingsRowContent(
                        title = stringResource(R.string.blur_intensity),
                        icon = R.drawable.blur
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${blur.roundToInt()}%",
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    WavySlider(
                        value = blur,
                        onValueChange = { blur = it },
                        onValueChangeFinished = {
                            onHandleConversationSettingsActions(
                                ConversationSettingActions.UpsertConversationSettings(
                                    state.settings.copy(
                                        wallpaperBlurIntensity = blur.roundToInt(),
                                    )
                                )
                            )
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                }
            }
        }
        SettingsGroup(title = stringResource(R.string.chat_color)) {
            SettingsItem(onClick = { showColorPicker = true }) {
                SettingsRowContent(
                    title = stringResource(R.string.chat_color),
                    description = stringResource(R.string.chat_color_desc),
                    icon = R.drawable.palette
                ) {
                    ColorSwatch(color = state.settings.color)
                }
            }
        }
    }

    Scaffold(
        bottomBar = { CuteNavigationButton(onNavigateUp = onNavigateBack) }
    ) { paddingValues ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    preview(Modifier.fillMaxHeight().aspectRatio(0.72f))
                }
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    content = settings
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp)
            ) {
                preview(
                    Modifier
                        .selfAlignHorizontally()
                        .padding(top = 16.dp)
                        .width(240.dp)
                        .height(340.dp)
                )
                settings()
            }
        }
    }
}

@Composable
private fun ChatPreview(
    wallpaper: Uri?,
    blur: Int,
    seedColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSystemDark = isSystemInDarkTheme()
    val theme by rememberAppTheme()
    val paletteStyle by rememberPaletteStyle()
    val isDark = when (theme) {
        CuteTheme.DARK, CuteTheme.AMOLED -> true
        CuteTheme.SYSTEM -> isSystemDark
        else -> false
    }
    val themeState = rememberDynamicMaterialThemeState(
        seedColor = seedColor ?: MaterialTheme.colorScheme.primary,
        isDark = isDark,
        isAmoled = theme == CuteTheme.AMOLED,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = paletteStyle.toPaletteStyle()
    )
    val transition = rememberInfiniteTransition()
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing))
    )

    DynamicMaterialExpressiveTheme(state = themeState, animate = true) {
        val colors = MaterialTheme.colorScheme

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(36.dp),
            color = colors.background,
            modifier = modifier
        ) {
            Box(Modifier.fillMaxSize()) {
                if (wallpaper != null) {
                    AsyncImage(
                        model = wallpaper,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .cloudy(blur, blur > 0)
                    )
                } else {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-30).dp)
                            .size(160.dp)
                            .graphicsLayer { rotationZ = spin }
                            .background(colors.primaryContainer.copy(alpha = 0.5f), MaterialShapes.Cookie12Sided.toShape())
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-30).dp, y = 30.dp)
                            .size(110.dp)
                            .graphicsLayer { rotationZ = -spin }
                            .background(colors.tertiaryContainer.copy(alpha = 0.5f), MaterialShapes.Cookie9Sided.toShape())
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    PreviewBubble(R.string.preview_received_1, MessageType.RECEIVED, SandwichPosition.TOP, colors.surfaceContainerHigh)
                    PreviewBubble(R.string.preview_received_2, MessageType.RECEIVED, SandwichPosition.BOTTOM, colors.surfaceContainerHigh)
                    Spacer(Modifier.height(6.dp))
                    PreviewBubble(R.string.preview_sent, MessageType.SENT, SandwichPosition.SOLO, colors.primary)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(colors.surfaceContainerHighest, CircleShape)
                            )
                            Spacer(Modifier.weight(1f))
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(colors.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PreviewBubble(
    @StringRes text: Int,
    type: MessageType,
    position: SandwichPosition,
    color: Color
) {
    Box(
        Modifier
            .align(if (type == MessageType.RECEIVED) Alignment.Start else Alignment.End)
            .widthIn(max = 180.dp)
    ) {
        TextBubbleContent(
            text = stringResource(text),
            type = type,
            isScheduled = false,
            sandwichPosition = position,
            bubbleColor = color
        )
    }
}

@Composable
private fun WallpaperActions(
    hasWallpaper: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val height = ButtonDefaults.MediumContainerHeight
    val removeProgress by animateFloatAsState(
        targetValue = if (hasWallpaper) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp)
            .height(height)
    ) {
        Button(
            onClick = onPick,
            shapes = if (hasWallpaper) {
                ButtonShapes(
                    shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape
                )
            } else ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier = Modifier
                .weight(2f)
                .height(height)
        ) {
            Icon(
                painter = painterResource(R.drawable.gallery),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.MediumIconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                text = stringResource(if (hasWallpaper) R.string.change_wallpaper else R.string.pick_wallpaper),
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 1
            )
        }
        if (removeProgress > 0.01f) {
            FilledTonalButton(
                onClick = onRemove,
                shapes = ButtonShapes(
                    shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape
                ),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .weight(removeProgress.coerceAtLeast(0.01f))
                    .height(height)
                    .graphicsLayer {
                        alpha = removeProgress.coerceIn(0f, 1f)
                        clip = true
                    }
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_filled),
                    contentDescription = stringResource(R.string.remove_wallpaper),
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize)
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color?) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(color ?: colors.surfaceContainerHighest)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .background(fill, MaterialShapes.Flower.toShape())
    ) {
        Icon(
            painter = painterResource(if (color != null) R.drawable.edit_filled else R.drawable.colorize),
            contentDescription = null,
            tint = if (color != null) {
                if (color.luminance() > 0.5f) Color.Black else Color.White
            } else colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
