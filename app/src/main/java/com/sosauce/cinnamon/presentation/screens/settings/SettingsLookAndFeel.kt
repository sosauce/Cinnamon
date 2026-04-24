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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberAppTheme
import com.sosauce.cinnamon.data.datastore.rememberGroupSubsequentCalls
import com.sosauce.cinnamon.data.datastore.rememberShowCharCount
import com.sosauce.cinnamon.presentation.screens.settings.components.LazyRowWithScrollButton
import com.sosauce.cinnamon.presentation.screens.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.presentation.screens.settings.components.SwitchSettingsCards
import com.sosauce.cinnamon.presentation.screens.settings.components.ThemeSelector
import com.sosauce.cinnamon.presentation.shared_components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.utils.CuteTheme
import com.sosauce.cinnamon.utils.anyDarkColorScheme
import com.sosauce.cinnamon.utils.anyLightColorScheme

@Composable
fun SettingsLookAndFeel() {

    var theme by rememberAppTheme()
    val isSystemDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    var showCharCount by rememberShowCharCount()
    var groupSubCalls by rememberGroupSubsequentCalls()


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

        SettingsWithTitle(title = R.string.messages) {
            SwitchSettingsCards(
                checked = showCharCount,
                onCheckedChange = { showCharCount = !showCharCount },
                topDp = 24.dp,
                bottomDp = 24.dp,
                text = "Show character count on typing"
            )
        }

        SettingsWithTitle(title = R.string.phone) {
            SwitchSettingsCards(
                checked = groupSubCalls,
                onCheckedChange = { groupSubCalls = !groupSubCalls },
                topDp = 24.dp,
                bottomDp = 24.dp,
                text = "Group subsequent calls"
            )
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