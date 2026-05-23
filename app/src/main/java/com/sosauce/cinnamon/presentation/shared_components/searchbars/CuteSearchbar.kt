@file:OptIn(
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.presentation.shared_components.searchbars

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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.shared_components.ScreenSelection
import com.sosauce.cinnamon.presentation.theme.nunitoFontFamily
import com.sosauce.cinnamon.utils.LocalScreen
import com.sosauce.cinnamon.utils.rememberSearchbarMaxFloatValue
import com.sosauce.cinnamon.utils.rememberSearchbarRightPadding
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@Composable
fun CuteSearchbar(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    sortingMenu: @Composable (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    showSearchField: Boolean = true,
    fab: @Composable (() -> Unit)? = null,
    onNavigate: (Screen) -> Unit,
) {

    val screenToLeadingIcon =
        mapOf(
            Screen.Messages to R.drawable.messages_filled,
            Screen.Contacts to R.drawable.contacts_filled,
            Screen.Dialer to R.drawable.phone_filled
        )
    val currentScreen = LocalScreen.current
    var isInScreenSelectionMode by remember { mutableStateOf(false) }



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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            this@Column.AnimatedVisibility(
                visible = showSearchField
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(6.dp)
                ) {
                    SharedTransitionLayout {
                        AnimatedContent(
                            targetState = isInScreenSelectionMode,
                            //transitionSpec = { slideInVertically(bouncySpec()) { it } + fadeIn() togetherWith slideOutVertically(bouncySpec()) { it } + fadeOut() }
                        ) {
                            if (it) {
                                ScreenSelection(
                                    onNavigate = onNavigate,
                                    dismiss = { isInScreenSelectionMode = false },
                                    animatedVisibilityScope = this
                                )
                            } else {
                                TextField(
                                    state = textFieldState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState("yeah"),
                                            animatedVisibilityScope = this,
                                        ),
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
                                        IconButton(
                                            onClick = { isInScreenSelectionMode = true },
                                            shapes = IconButtonDefaults.shapes()
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    screenToLeadingIcon[currentScreen]
                                                        ?: R.drawable.search
                                                ),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        Row {
                                            sortingMenu?.invoke()
                                            IconButton(
                                                onClick = { onNavigate(Screen.Settings) },
                                                shapes = IconButtonDefaults.shapes()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.settings_filled),
                                                    contentDescription = stringResource(R.string.settings)
                                                )
                                            }
                                        }
                                    },
                                    textStyle = TextStyle.Default.copy(
                                        fontFamily = nunitoFontFamily
                                    ),
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
}






