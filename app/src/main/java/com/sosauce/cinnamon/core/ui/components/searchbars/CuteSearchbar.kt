@file:OptIn(
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.core.ui.components.searchbars

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.ScreenSelection
import com.sosauce.cinnamon.core.ui.nunitoFontFamily
import com.sosauce.cinnamon.core.utils.LocalScreen
import com.sosauce.cinnamon.core.utils.rememberSearchbarMaxFloatValue
import com.sosauce.cinnamon.core.utils.rememberSearchbarRightPadding
import com.sosauce.cinnamon.core.utils.thenIf
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@Composable
fun CuteSearchbar(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    sortingMenu: @Composable (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    fab: @Composable (() -> Unit)? = null,
    onNavigate: (Screen) -> Unit,
) {

    val screenToLeadingIcon =
        mapOf(
            Screen.Conversations to R.drawable.messages_filled,
            Screen.Contacts to R.drawable.contacts_filled,
            Screen.Dialer to R.drawable.phone_filled
        )
    val currentScreen = LocalScreen.current
    var isInScreenSelectionMode by remember { mutableStateOf(false) }
    val isSearching = textFieldState.text.isNotEmpty()


    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(rememberSearchbarMaxFloatValue())
            .padding(end = rememberSearchbarRightPadding())
            .imePadding()

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            navigationIcon?.invoke()
            Spacer(Modifier.weight(1f))
            fab?.invoke()
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 5.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            SharedTransitionLayout(
                modifier = Modifier.padding(6.dp)
            ) {
                AnimatedContent(
                    targetState = isInScreenSelectionMode,
                ) {
                    CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
                        if (it) {
                            ScreenSelection(
                                onNavigate = onNavigate,
                                dismiss = { isInScreenSelectionMode = false }
                            )
                        } else {
                            TextField(
                                state = textFieldState,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.search_here),
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {

                                    AnimatedContent(
                                        targetState = isSearching
                                    ) {
                                        if (it) {
                                            Icon(
                                                painter = painterResource(R.drawable.search),
                                                contentDescription = null
                                            )
                                        } else {
                                            val icon = screenToLeadingIcon[currentScreen]
                                                ?: R.drawable.search

                                            IconButton(
                                                onClick = {
                                                    isInScreenSelectionMode = true
                                                },
                                                shapes = IconButtonDefaults.shapes()
                                            ) {
                                                Icon(
                                                    painter = painterResource(icon),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .sharedElement(
                                                            sharedContentState = rememberSharedContentState(
                                                                icon
                                                            ),
                                                            animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                                        )
                                                )
                                            }
                                        }
                                    }

                                },
                                trailingIcon = {
                                    AnimatedContent(
                                        targetState = isSearching
                                    ) {
                                        if (it) {
                                            IconButton(
                                                onClick = textFieldState::clearText,
                                                shapes = IconButtonDefaults.shapes()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.cancel_filled),
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            Row {
                                                sortingMenu?.invoke()
                                                IconButton(
                                                    onClick = { onNavigate(Screen.Settings) },
                                                    shapes = IconButtonDefaults.shapes()
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.settings_filled),
                                                        contentDescription = stringResource(
                                                            R.string.settings
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                lineLimits = TextFieldLineLimits.SingleLine,
                                shape = FloatingToolbarDefaults.ContainerShape
                            )
                        }
                    }

                }
            }
        }
    }
}


object CuteSearchbarDefaults {

}






