package com.sosauce.cinnamon.presentation.screens.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.sosauce.cinnamon.data.datastore.rememberAppTheme
import com.sosauce.cinnamon.data.datastore.rememberPaletteStyle
import com.sosauce.cinnamon.data.datastore.rememberUseSystemFont
import com.sosauce.cinnamon.presentation.screens.settings.components.FontSelector
import com.sosauce.cinnamon.presentation.screens.settings.components.LazyRowWithScrollButton
import com.sosauce.cinnamon.presentation.screens.settings.components.PaletteSelector
import com.sosauce.cinnamon.presentation.screens.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.presentation.screens.settings.components.ThemeSelector
import com.sosauce.cinnamon.presentation.theme.nunitoFontFamily
import com.sosauce.cinnamon.utils.CutePaletteStyle
import com.sosauce.cinnamon.utils.CuteTheme
import com.sosauce.cinnamon.utils.anyDarkColorScheme
import com.sosauce.cinnamon.utils.anyLightColorScheme

@Composable
fun SettingsLookAndFeel() {

    var theme by rememberAppTheme()
    val isSystemDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    var useSystemFont by rememberUseSystemFont()
    var paletteStyle by rememberPaletteStyle()


    val themeItems = listOf(
        ThemeItem(
            onClick = { theme = CuteTheme.SYSTEM },
            backgroundColor = if (isSystemDark) anyDarkColorScheme().background else anyLightColorScheme().background,
            text = stringResource(R.string.system),
            isSelected = theme == CuteTheme.SYSTEM,
            iconAndTint = Pair(
                R.drawable.system_theme,
                if (isSystemDark) anyDarkColorScheme().onBackground else anyLightColorScheme().onBackground
            )
        ),
        ThemeItem(
            onClick = { theme = CuteTheme.DARK },
            backgroundColor = anyDarkColorScheme().background,
            text = stringResource(R.string.dark),
            isSelected = theme == CuteTheme.DARK,
            iconAndTint = Pair(
                R.drawable.dark_mode,
                anyDarkColorScheme().onBackground
            )
        ),
        ThemeItem(
            onClick = { theme = CuteTheme.LIGHT },
            backgroundColor = anyLightColorScheme().background,
            text = stringResource(R.string.light),
            isSelected = theme == CuteTheme.LIGHT,
            iconAndTint = Pair(
                R.drawable.light_mode,
                anyLightColorScheme().onBackground
            )
        ),
        ThemeItem(
            onClick = { theme = CuteTheme.AMOLED },
            backgroundColor = Color.Black,
            text = stringResource(R.string.amoled),
            isSelected = theme == CuteTheme.AMOLED,
            iconAndTint = Pair(R.drawable.amoled, Color.White)
        )
    )

    val fontItems = listOf(
        FontItem(
            onClick = { useSystemFont = false },
            fontStyle = FontStyle.DEFAULT,
            borderColor = if (!useSystemFont) MaterialTheme.colorScheme.primary else Color.Transparent,
            text = {
                Text(
                    text = "Tt",
                    fontFamily = nunitoFontFamily
                )
            },
        ),
        FontItem(
            onClick = { useSystemFont = true },
            fontStyle = FontStyle.SYSTEM,
            borderColor = if (useSystemFont) MaterialTheme.colorScheme.primary else Color.Transparent,
            text = {
                Text(
                    text = "Tt",
                    style = TextStyle.Default
                )
            }
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

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
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
                    ThemeSelector(theme)
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
                    FontSelector(font)
                }
            }
        }

    }
}

data class ThemeItem(
    val onClick: () -> Unit,
    val backgroundColor: Color,
    val text: String,
    val isSelected: Boolean,
    val iconAndTint: Pair<Int, Color>
)

data class FontItem(
    val onClick: () -> Unit,
    val fontStyle: FontStyle,
    val borderColor: Color,
    val text: @Composable () -> Unit
)

enum class FontStyle {
    DEFAULT,
    SYSTEM
}