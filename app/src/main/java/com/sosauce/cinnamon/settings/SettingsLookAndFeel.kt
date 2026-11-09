package com.sosauce.cinnamon.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberAppTheme
import com.sosauce.cinnamon.core.datastore.rememberIncomingCallFullscreen
import com.sosauce.cinnamon.core.datastore.rememberPaletteStyle
import com.sosauce.cinnamon.core.datastore.rememberUseSystemFont
import com.sosauce.cinnamon.core.utils.CutePaletteStyle
import com.sosauce.cinnamon.core.utils.CuteTheme
import com.sosauce.cinnamon.core.utils.anyDarkColorScheme
import com.sosauce.cinnamon.core.utils.anyLightColorScheme
import com.sosauce.cinnamon.settings.components.PaletteTile
import com.sosauce.cinnamon.settings.components.SelectableTile
import com.sosauce.cinnamon.settings.components.SettingsGroup
import com.sosauce.cinnamon.settings.components.SettingsPicker
import com.sosauce.cinnamon.settings.components.SettingsSwitch

@Composable
fun SettingsLookAndFeel() {

    var theme by rememberAppTheme()
    var useSystemFont by rememberUseSystemFont()
    var paletteStyle by rememberPaletteStyle()
    var incomingFullscreen by rememberIncomingCallFullscreen()
    val isSystemDark = isSystemInDarkTheme()
    val anyDark = anyDarkColorScheme()
    val anyLight = anyLightColorScheme()

    val themes = listOf(
        ThemeOption(
            value = CuteTheme.SYSTEM,
            label = R.string.system,
            icon = R.drawable.system_theme,
            background = if (isSystemDark) anyDark.surfaceContainerHighest else anyLight.surfaceContainerHighest,
            iconColor = if (isSystemDark) anyDark.onSurface else anyLight.onSurface
        ),
        ThemeOption(
            value = CuteTheme.DARK,
            label = R.string.dark,
            icon = R.drawable.dark_mode,
            background = anyDark.surfaceContainerHighest,
            iconColor = anyDark.onSurface
        ),
        ThemeOption(
            value = CuteTheme.LIGHT,
            label = R.string.light,
            icon = R.drawable.light_mode,
            background = anyLight.surfaceContainerHighest,
            iconColor = anyLight.onSurface
        ),
        ThemeOption(
            value = CuteTheme.AMOLED,
            label = R.string.amoled,
            icon = R.drawable.amoled,
            background = Color.Black,
            iconColor = Color.White
        )
    )

    val palettes = listOf(
        CutePaletteStyle.TONAL_SPOT,
        CutePaletteStyle.EXPRESSIVE,
        CutePaletteStyle.VIBRANT,
        CutePaletteStyle.FIDELITY,
        CutePaletteStyle.NEUTRAL,
        CutePaletteStyle.MONOCHROME,
        CutePaletteStyle.FRUIT_SALAD
    )

    SettingsGroup(title = stringResource(R.string.appearance)) {
        SettingsPicker(title = stringResource(R.string.theme)) {
            items(themes) { option ->
                SelectableTile(
                    selected = theme == option.value,
                    onClick = { theme = option.value },
                    label = stringResource(option.label),
                    containerColor = option.background
                ) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = null,
                        tint = option.iconColor
                    )
                }
            }
        }
        SettingsPicker(title = stringResource(R.string.palette)) {
            items(palettes) { palette ->
                PaletteTile(
                    paletteStyle = palette,
                    selected = palette == paletteStyle,
                    onClick = { paletteStyle = palette }
                )
            }
        }
        SettingsPicker(title = stringResource(R.string.font)) {
            item {
                FontTile(
                    selected = !useSystemFont,
                    onClick = { useSystemFont = false },
                    icon = R.drawable.match_case,
                    label = R.string.default_text
                )
            }
            item {
                FontTile(
                    selected = useSystemFont,
                    onClick = { useSystemFont = true },
                    icon = R.drawable.system_font,
                    label = R.string.system
                )
            }
        }
    }

    SettingsGroup(title = stringResource(R.string.calls)) {
        SettingsSwitch(
            title = stringResource(R.string.incoming_call_fullscreen),
            description = stringResource(R.string.incoming_call_fullscreen_desc),
            checked = incomingFullscreen,
            onCheckedChange = { incomingFullscreen = !incomingFullscreen }
        )
    }
}

@Composable
private fun FontTile(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    @StringRes label: Int
) {
    SelectableTile(
        selected = selected,
        onClick = onClick,
        label = stringResource(label)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp)
        )
    }
}

private data class ThemeOption(
    val value: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val background: Color,
    val iconColor: Color
)
