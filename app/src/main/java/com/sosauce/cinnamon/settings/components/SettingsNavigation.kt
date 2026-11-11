package com.sosauce.cinnamon.settings.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberInitialScreen
import com.sosauce.cinnamon.core.utils.DefaultTabOption
import com.sosauce.nekobites.components.settings.SelectableTile
import com.sosauce.nekobites.components.settings.SettingsGroup
import com.sosauce.nekobites.components.settings.SettingsPicker

@Composable
fun SettingsNavigation() {

    var initialScreen by rememberInitialScreen()

    val screens = listOf(
        ScreenOption(DefaultTabOption.MESSAGES, R.drawable.messages_filled, R.string.messages),
        ScreenOption(DefaultTabOption.CONTACTS, R.drawable.contacts_filled, R.string.contacts),
        ScreenOption(DefaultTabOption.DIALER, R.drawable.phone_filled, R.string.dialer),
        ScreenOption(DefaultTabOption.DIALPAD, R.drawable.dialpad, R.string.dialpad)
    )

    SettingsGroup(title = stringResource(R.string.navigation)) {
        SettingsPicker(
            title = stringResource(R.string.default_launch_screen),
            description = stringResource(R.string.default_tab)
        ) {
            items(screens, key = { it.value }) { screen ->
                val selected = initialScreen == screen.value
                SelectableTile(
                    selected = selected,
                    onClick = { initialScreen = screen.value },
                    label = stringResource(screen.label),
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Icon(
                        painter = painterResource(screen.icon),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class ScreenOption(
    val value: Int,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int
)
