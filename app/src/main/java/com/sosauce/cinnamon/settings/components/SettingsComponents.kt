@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.CuteTheme
import com.sosauce.cinnamon.core.datastore.rememberAppTheme
import com.sosauce.cinnamon.core.utils.toPaletteStyle

private val GroupCorner = 24.dp
private val ItemCorner = 2.dp
private val ItemPressedCorner = 22.dp
private val GroupGap = 2.dp

@Composable
fun SettingsPage(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
        content = content
    )
}

@Composable
fun SettingsGroupLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) SettingsGroupLabel(title) else Spacer(Modifier.height(16.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(GroupGap),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(GroupCorner)),
            content = content
        )
    }
}

fun settingsItemShape(isFirst: Boolean, isLast: Boolean): Shape = RoundedCornerShape(
    topStart = if (isFirst) GroupCorner else ItemCorner,
    topEnd = if (isFirst) GroupCorner else ItemCorner,
    bottomStart = if (isLast) GroupCorner else ItemCorner,
    bottomEnd = if (isLast) GroupCorner else ItemCorner
)

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    baseShape: Shape? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) ItemPressedCorner else ItemCorner,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "itemCorner"
    )
    val shape = if (isPressed || baseShape == null) RoundedCornerShape(corner) else baseShape
    val color = MaterialTheme.colorScheme.surfaceContainer

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = color,
            interactionSource = interactionSource,
            modifier = modifier.fillMaxWidth()
        ) { Column(content = content) }
    } else {
        Surface(
            shape = baseShape ?: RoundedCornerShape(ItemCorner),
            color = color,
            modifier = modifier.fillMaxWidth()
        ) { Column(content = content) }
    }
}

@Composable
fun SettingsIconBadge(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: Shape = MaterialShapes.Cookie9Sided.toShape()
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .background(container, shape)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SettingsRowContent(
    title: String,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        icon?.let {
            SettingsIconBadge(icon = it)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.let {
            Spacer(Modifier.width(12.dp))
            it()
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    baseShape: Shape? = null
) {
    SettingsItem(
        modifier = modifier,
        onClick = onCheckedChange,
        baseShape = baseShape
    ) {
        SettingsRowContent(
            title = title,
            description = description,
            icon = icon
        ) {
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
                colors = SwitchDefaults.colors(uncheckedBorderColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun SettingsClickable(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    baseShape: Shape? = null
) {
    SettingsItem(
        modifier = modifier,
        onClick = onClick,
        baseShape = baseShape
    ) {
        SettingsRowContent(
            title = title,
            description = description,
            icon = icon,
        )
    }
}

@Composable
fun <T> SettingsChoice(
    title: String,
    active: T,
    options: List<SettingOption<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val colors = MaterialTheme.colorScheme

    SettingsItem(modifier = modifier) {
        SettingsRowContent(title = title, description = description)
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            options.fastForEachIndexed { index, option ->
                val selected = option.value == active
                ToggleButton(
                    checked = selected,
                    onCheckedChange = { onSelect(option.value) },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    colors = ToggleButtonColors(
                        containerColor = colors.surfaceContainerHighest,
                        contentColor = colors.onSurface,
                        disabledContainerColor = colors.onSurface.copy(alpha = 0.1f),
                        disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
                        checkedContainerColor = colors.primary,
                        checkedContentColor = colors.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (selected) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    }
                    Text(stringResource(option.labelRes))
                }
            }
        }
    }
}

@Composable
fun SettingsPicker(
    title: String?,
    modifier: Modifier = Modifier,
    description: String? = null,
    tiles: LazyListScope.() -> Unit
) {
    SettingsItem(modifier = modifier) {
        if (title != null) SettingsRowContent(title = title, description = description)
        LazyRow(
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = if (title == null) 12.dp else 0.dp,
                bottom = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = tiles
        )
    }
}

@Composable
fun SelectableTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    width: Dp = 64.dp,
    height: Dp = 64.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val corner by animateDpAsState(if (selected) height / 2 else 18.dp, spec, label = "tileCorner")
    val ringColor by animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        label = "tileRing"
    )
    val labelColor by animateColorAsState(
        if (selected) colors.primary else colors.onSurfaceVariant,
        label = "tileLabel"
    )
    val ringShape = RoundedCornerShape(corner + 5.dp)
    val shape = RoundedCornerShape(corner)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .border(2.5.dp, ringColor, ringShape)
                .padding(5.dp)
                .size(width = width, height = height)
                .clip(shape)
                .background(containerColor),
            content = content
        )
        if (label != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = if (selected) MaterialTheme.typography.labelLargeEmphasized
                else MaterialTheme.typography.labelLarge,
                color = labelColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PaletteTile(
    paletteStyle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val theme by rememberAppTheme()
    val isDark = when (theme) {
        CuteTheme.DARK, CuteTheme.AMOLED -> true
        CuteTheme.SYSTEM -> isSystemDark
        else -> false
    }
    val state = rememberDynamicMaterialThemeState(
        seedColor = MaterialTheme.colorScheme.primary,
        isDark = isDark,
        isAmoled = theme == CuteTheme.AMOLED,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = paletteStyle.toPaletteStyle()
    )

    SelectableTile(
        selected = selected,
        onClick = onClick,
        label = paletteStyle
    ) {
        DynamicMaterialExpressiveTheme(state = state, animate = true) {
            val scheme = MaterialTheme.colorScheme
            val quarters = listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.primaryContainer)
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val d = size.minDimension
                quarters.fastForEachIndexed { i, color ->
                    drawArc(
                        color = color,
                        startAngle = 180f + i * 90f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset((size.width - d) / 2, (size.height - d) / 2),
                        size = Size(d, d)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsEmptyState(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    baseShape: Shape? = null
) {
    SettingsItem(modifier = modifier, baseShape = baseShape) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialShapes.Cookie12Sided.toShape()
                    )
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class SettingOption<T>(
    val value: T,
    @param:StringRes val labelRes: Int
)
