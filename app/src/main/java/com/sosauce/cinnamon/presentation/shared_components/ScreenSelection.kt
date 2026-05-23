@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.shared_components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.utils.LocalScreen
import com.sosauce.cinnamon.utils.rememberInteractionSource
import com.sosauce.cinnamon.utils.thenIf

@Composable
fun SharedTransitionScope.ScreenSelection(
    onNavigate: (Screen) -> Unit,
    dismiss: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {

    val interactionsSources = List(4) { rememberInteractionSource() }
    val currentScreen = LocalScreen.current
    val screens = listOf(
        ScreenCategory(
            screen = Screen.Messages,
            onClick = { onNavigate(Screen.Messages) },
            unselectedIcon = R.drawable.message_rounded,
            selectedIcon = R.drawable.messages_filled
        ),
        ScreenCategory(
            screen = Screen.Contacts,
            onClick = { onNavigate(Screen.Contacts) },
            unselectedIcon = R.drawable.contacts,
            selectedIcon = R.drawable.contacts_filled
        ),
        ScreenCategory(
            screen = Screen.Dialer,
            onClick = { onNavigate(Screen.Dialer) },
            unselectedIcon = R.drawable.phone,
            selectedIcon = R.drawable.phone_filled
        )
    )
    val key = rememberSharedContentState(key = "yeah")

    ButtonGroup(
        modifier = Modifier.fillMaxWidth()
    ) {
        screens.fastForEachIndexed { index, category ->
            val isActive = currentScreen == category.screen

            val containerColor =
                if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent

            Button(
                onClick = {
                    category.onClick()
                    dismiss()
                },
                interactionSource = interactionsSources[index],
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColorFor(containerColor)
                ),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = TextFieldDefaults.MinHeight,
                    )
                    .weight(1f)
                    .animateWidth(interactionsSources[index])
                    .thenIf(isActive) {
                        sharedElement(
                            sharedContentState = key,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
            ) {
                val icon =
                    if (isActive) category.selectedIcon else category.unselectedIcon

                Icon(
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }

//            ToggleButton(
//                checked = currentScreen == category.screen,
//                onCheckedChange = {
//                    category.onClick()
//                    dismiss()
//                },
//                shapes =
//                    when (index) {
//                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
//                        screens.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
//                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
//                    },
//                interactionSource = interactionsSources[index],
//                modifier = Modifier
//                    .weight(1f)
//                    .animateWidth(interactionsSources[index])
//                    .thenIf(currentScreen == category.screen) {
//                        sharedElement(
//                            sharedContentState = key,
//                            animatedVisibilityScope = animatedVisibilityScope,
//                        )
//                    }
//            ) {
//                val icon =
//                    if (currentScreen == category.screen) category.selectedIcon else category.unselectedIcon
//
//                Icon(
//                    painter = painterResource(icon),
//                    contentDescription = null
//                )
//            }
        }
    }
}

private data class ScreenCategory(
    val screen: Screen,
    val onClick: () -> Unit,
    @param:DrawableRes val unselectedIcon: Int,
    @param:DrawableRes val selectedIcon: Int
)