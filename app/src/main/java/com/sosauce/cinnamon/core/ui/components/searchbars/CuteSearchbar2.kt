package com.sosauce.cinnamon.core.ui.components.searchbars

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.ScreenSelection

@Composable
fun CuteSearchbar2(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    sortingMenu: @Composable (() -> Unit)? = null,
    navigationButton: @Composable (() -> Unit)? = null,
    fab: @Composable (() -> Unit)? = null,
    backButton: @Composable (() -> Unit)? = null,
    onNavigate: (Screen) -> Unit,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(0.9f)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                navigationButton?.invoke()
                Spacer(Modifier.weight(1f))
                fab?.invoke()
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            state = textFieldState,
                            modifier = Modifier.weight(1f),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            shape = FloatingToolbarDefaults.ContainerShape,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_here),
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                Row {

                                    AnimatedContent(
                                        targetState = textFieldState.text.isNotEmpty()
                                    ) {
                                        if (it) {
                                            IconButton(
                                                onClick = textFieldState::clearText,
                                                shapes = IconButtonDefaults.shapes()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.cancel_filled),
                                                    contentDescription = stringResource(R.string.clear_search)
                                                )
                                            }
                                        }
                                        else {
                                            sortingMenu?.invoke()
                                        }
                                    }
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
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        navigationButton?.invoke()
                        SharedTransitionLayout(modifier = Modifier.weight(1f)) {
                            ScreenSelection(
                                onNavigate = onNavigate,
                                dismiss = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
