@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.toolbars.ToolbarSkeleton
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationSearchState
import com.sosauce.nekobites.animations.AnimatedCounter

@Composable
fun ConversationSearchTopBar(
    textFieldState: TextFieldState,
    search: ConversationSearchState,
    onHandleConversationActions: (ConversationActions) -> Unit
) {

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ToolbarSkeleton {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledIconButton(
                onClick = { onHandleConversationActions(ConversationActions.CloseSearch) },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close_search)
                )
            }
            Spacer(Modifier.width(6.dp))
            TextField(
                state = textFieldState,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .animateContentSize(),
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = FloatingToolbarDefaults.ContainerShape,
                textStyle = MaterialTheme.typography.bodyLargeEmphasized,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_conversation),
                        maxLines = 1
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = textFieldState.text.isNotEmpty(),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
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
                }
            )
            AnimatedVisibility(
                visible = search.query.isNotBlank(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {

                val style = MaterialTheme.typography.labelLargeEmphasized.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    AnimatedCounter(
                        count = search.currentIndex + 1,
                        textStyle = style
                    )
                    Text(
                        text = "/",
                        style = style
                    )
                    AnimatedCounter(
                        count = search.matches.size,
                        textStyle = style
                    )
                }

//                AnimatedContent(
//                    targetState = if (search.matches.isEmpty()) 0 to 0 else search.currentIndex + 1 to search.matches.size
//                ) { (current, total) ->
//                    Text(
//                        text = stringResource(R.string.search_match_counter, current, total),
//                        style = MaterialTheme.typography.labelLargeEmphasized,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = 1,
//                        modifier = Modifier.padding(horizontal = 8.dp)
//                    )
//                }
            }
            IconButton(
                onClick = { onHandleConversationActions(ConversationActions.ShowOlderSearchMatch) },
                enabled = search.hasOlderMatch,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.up),
                    contentDescription = stringResource(R.string.older_match)
                )
            }
            IconButton(
                onClick = { onHandleConversationActions(ConversationActions.ShowNewerSearchMatch) },
                enabled = search.hasNewerMatch,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.down),
                    contentDescription = stringResource(R.string.newer_match)
                )
            }
        }
    }
}
