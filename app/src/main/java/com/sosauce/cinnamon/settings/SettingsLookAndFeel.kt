package com.sosauce.cinnamon.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberAppTheme
import com.sosauce.cinnamon.core.datastore.rememberIncomingCallFullscreen
import com.sosauce.cinnamon.core.datastore.rememberPaletteStyle
import com.sosauce.cinnamon.core.datastore.rememberUseSystemFont
import com.sosauce.cinnamon.core.ui.nunitoFontFamily
import com.sosauce.cinnamon.core.utils.CutePaletteStyle
import com.sosauce.cinnamon.core.utils.CuteTheme
import com.sosauce.cinnamon.core.utils.anyDarkColorScheme
import com.sosauce.cinnamon.core.utils.anyLightColorScheme
import com.sosauce.cinnamon.settings.components.PaletteSelector
import com.sosauce.cinnamon.settings.components.SettingsSelector
import com.sosauce.cinnamon.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.settings.components.SwitchSettingsCard
import com.sosauce.nekobites.components.LazyRowWithScrollButton

@Composable
fun SettingsLookAndFeel() {

    var theme by rememberAppTheme()
    val isSystemDark = isSystemInDarkTheme()
    var useSystemFont by rememberUseSystemFont()
    var paletteStyle by rememberPaletteStyle()
    val anyDark = anyDarkColorScheme()
    val anyLight = anyLightColorScheme()


    val themeItems = listOf(
        ThemeItem(
            onClick = { theme = CuteTheme.SYSTEM },
            backgroundColor = if (isSystemDark) anyDark.surface else anyLight.surface,
            iconColor = if (isSystemDark) anyDark.onSurface else anyLight.onSurface,
            text = R.string.system,
            isSelected = theme == CuteTheme.SYSTEM,
            icon = R.drawable.system_theme
        ),
        ThemeItem(
            onClick = { theme = CuteTheme.DARK },
            backgroundColor = anyDark.surface,
            iconColor = anyDark.onSurface,
            text = R.string.dark,
            isSelected = theme == CuteTheme.DARK,
            icon = R.drawable.dark_mode
        ),
        ThemeItem(
            onClick = { theme = CuteTheme.LIGHT },
            backgroundColor = anyLight.surface,
            iconColor = anyLight.onSurface,
            text = R.string.light,
            icon = R.drawable.light_mode,
            isSelected = theme == CuteTheme.LIGHT
        ),
        ThemeItem(
            onClick = { theme = CuteTheme.AMOLED },
            backgroundColor = Color.Black,
            iconColor = Color.White,
            text = R.string.amoled,
            icon = R.drawable.amoled,
            isSelected = theme == CuteTheme.AMOLED
        )
    )

    val fontItems = listOf(
        FontItem(
            onClick = { useSystemFont = false },
            isSelected = !useSystemFont,
            icon = R.drawable.match_case,
            text = R.string.default_text
        ),
        FontItem(
            onClick = { useSystemFont = true },
            isSelected = useSystemFont,
            icon = R.drawable.system_font,
            text = R.string.system
        )
    )

    val paletteItems = listOf(
        CutePaletteStyle.TONAL_SPOT,
        CutePaletteStyle.EXPRESSIVE,
        CutePaletteStyle.VIBRANT,
        CutePaletteStyle.FIDELITY,
        CutePaletteStyle.NEUTRAL,
        CutePaletteStyle.MONOCHROME,
        CutePaletteStyle.FRUIT_SALAD
    )

    var incomingFullscreen by rememberIncomingCallFullscreen()

    Column {
        SettingsWithTitle(
            title = R.string.theme
        ) {
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                LazyRowWithScrollButton(
                    items = themeItems
                ) { theme ->
                    SettingsSelector(
                        onClick = theme.onClick,
                        icon = theme.icon,
                        text = theme.text,
                        isSelected = theme.isSelected,
                        containerColor = theme.backgroundColor,
                        contentColor = theme.iconColor
                    )
                }
            }
        }

        SettingsWithTitle(
            title = R.string.palette
        ) {
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                LazyRowWithScrollButton(
                    items = paletteItems
                ) { palette ->
                    PaletteSelector(
                        isSelected = palette == paletteStyle,
                        onSelectNewPalette = { paletteStyle = palette },
                        paletteStyle = palette
                    )
                }
            }
        }

        SettingsWithTitle(
            title = R.string.font
        ) {
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                LazyRowWithScrollButton(
                    items = fontItems
                ) { font ->
                    SettingsSelector(
                        onClick = font.onClick,
                        icon = font.icon,
                        text = font.text,
                        isSelected = font.isSelected
                    )
                }
            }
        }

        // Incoming call popup — moved from Permissions (blur options removed per request)
        SettingsWithTitle(
            title = R.string.look_and_feel
        ) {
            SwitchSettingsCard(
                checked = incomingFullscreen,
                onCheckedChange = { incomingFullscreen = !incomingFullscreen },
                topDp = 24.dp,
                bottomDp = 24.dp,
                text = "Incoming call full-screen popup"
            )
        }

    }
}

data class ThemeItem(
    val onClick: () -> Unit,
    val backgroundColor: Color,
    val iconColor: Color = Color.White,
    val text: Int,
    val icon: Int,
    val isSelected: Boolean
)

data class FontItem(
    val onClick: () -> Unit,
    val icon: Int,
    val text: Int,
    val isSelected: Boolean
)

enum class FontStyle {
    DEFAULT,
    SYSTEM
}