@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.utils.LocalScreen

@Composable
fun SharedTransitionScope.ScreenSelection(
    onNavigate: (Screen) -> Unit,
    dismiss: () -> Unit
) {

    val currentScreen = LocalScreen.current
    val screens = listOf(
        ScreenCategory(
            screen = Screen.Conversations,
            name = R.string.messages,
            onClick = { onNavigate(Screen.Conversations) },
            unselectedIcon = R.drawable.message_rounded,
            selectedIcon = R.drawable.messages_filled
        ),
        ScreenCategory(
            screen = Screen.Contacts,
            name = R.string.contacts,
            onClick = { onNavigate(Screen.Contacts) },
            unselectedIcon = R.drawable.contacts,
            selectedIcon = R.drawable.contacts_filled
        ),
        ScreenCategory(
            screen = Screen.Dialer,
            name = R.string.dialer,
            onClick = { onNavigate(Screen.Dialer) },
            unselectedIcon = R.drawable.phone,
            selectedIcon = R.drawable.phone_filled
        )
    )
    ShortNavigationBar {
            screens.forEach { screen ->

                val haptic = LocalHapticFeedback.current
                val selected = currentScreen == screen.screen


                ShortNavigationBarItem(
                    selected = selected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        dismiss()
                        screen.onClick()
                    },
                    icon = {
                        val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(icon),
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                )
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(screen.name)
                        )
                    }
                )
            }
        }
}

private data class ScreenCategory(
    val screen: Screen,
    val onClick: () -> Unit,
    val name: Int,
    val unselectedIcon: Int,
    val selectedIcon: Int
)