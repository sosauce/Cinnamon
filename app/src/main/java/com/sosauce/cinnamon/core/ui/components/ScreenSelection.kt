@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.utils.LocalScreen
import com.sosauce.nekobites.helpers.rememberInteractionSource

@Composable
fun ScreenSelection(
    onNavigate: (Screen) -> Unit,
    dismiss: () -> Unit
) {

    val currentScreen = LocalScreen.current
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
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
            screen = Screen.Calls,
            name = R.string.calls,
            onClick = { onNavigate(Screen.Calls) },
            unselectedIcon = R.drawable.call_log,
            selectedIcon = R.drawable.call_log_filled
        ),
        ScreenCategory(
            screen = Screen.Dialer(),
            name = R.string.dialer,
            onClick = { if (currentScreen !is Screen.Dialer) onNavigate(Screen.Dialer()) },
            unselectedIcon = R.drawable.dialpad,
            selectedIcon = R.drawable.dialpad
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(TextFieldDefaults.MinHeight)
    ) {
        screens.forEach { screen ->
            // compared by type, the dialer can be opened with a prefilled number
            val selected = currentScreen == screen.screen::class
            val interactionSource = rememberInteractionSource()
            val isPressed by interactionSource.collectIsPressedAsState()
            val weight by animateFloatAsState(
                targetValue = when {
                    selected -> 2.2f
                    isPressed -> 1.5f
                    else -> 1f
                },
                animationSpec = motion.fastSpatialSpec()
            )
            val corner by animateDpAsState(
                targetValue = when {
                    isPressed -> 12.dp
                    selected -> 50.dp
                    else -> 18.dp
                },
                animationSpec = motion.fastSpatialSpec()
            )
            val containerColor by animateColorAsState(
                if (selected) colors.primary else colors.surfaceContainerHighest
            )
            val contentColor by animateColorAsState(
                if (selected) colors.onPrimary else colors.onSurfaceVariant
            )

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    dismiss()
                    screen.onClick()
                },
                shape = RoundedCornerShape(corner),
                color = containerColor,
                contentColor = contentColor,
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = stringResource(screen.name)
                    )
                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(screen.name),
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
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
