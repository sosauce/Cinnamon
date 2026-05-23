@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberSortConversationsAscending
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.messages.components.Conversation
import com.sosauce.cinnamon.presentation.screens.messages.components.PinnedConversation
import com.sosauce.cinnamon.presentation.shared_components.ConversationsSelectedBar
import com.sosauce.cinnamon.presentation.shared_components.NoXFound
import com.sosauce.cinnamon.presentation.shared_components.animations.AnimatedFab
import com.sosauce.cinnamon.presentation.shared_components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.utils.LazyListKeys
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import com.sosauce.sweetselect.SweetSelectState
import com.sosauce.sweetselect.rememberSweetSelectState

@Composable
fun SharedTransitionScope.ConversationsScreen(
    state: ConversationsState,
    onNavigate: (Screen) -> Unit,
    onHandleConversationsAction: (ConversationsAction) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val sweetSelectState = rememberSweetSelectState<CuteConversation>()
    var sortConversationsAscending by rememberSortConversationsAscending()

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {
        Scaffold(
            bottomBar = {
                AnimatedContent(
                    targetState = sweetSelectState.isInSelectionMode,
                ) {
                    if (!it) {
                        CuteSearchbar(
                            modifier = Modifier.selfAlignHorizontally(),
                            sortingMenu = {},
                            textFieldState = state.textFieldState,
                            fab = {
                                AnimatedFab(
                                    onClick = { onNavigate(Screen.StartConversation) }
                                )
                            },
                            onNavigate = onNavigate
                        )
                    } else {
                        ConversationsSelectedBar(
                            modifier = Modifier.selfAlignHorizontally(),
                            items = state.conversations,
                            multiSelectState = sweetSelectState,
                            onDeleteConversations = {
                                val threadIds = sweetSelectState.selectedItems.map { it.threadId }
                                onHandleConversationsAction(
                                    ConversationsAction.DeleteConversations(
                                        threadIds
                                    )
                                )
                                sweetSelectState.clearSelected()
                            },
                            onArchiveThreads = {
                                val threadIds = sweetSelectState.selectedItems.map { it.threadId }
                                onHandleConversationsAction(
                                    ConversationsAction.ArchiveConversations(
                                        threadIds
                                    )
                                )
                                sweetSelectState.clearSelected()
                            },
                            onPinThreads = {
                                val threadIds = sweetSelectState.selectedItems.map { it.threadId }
                                onHandleConversationsAction(
                                    ConversationsAction.PinConversations(
                                        threadIds
                                    )
                                )
                                sweetSelectState.clearSelected()
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = paddingValues,
                state = listState
            ) {
                if (state.hasArchivedThreads) {
                    item(LazyListKeys.ARCHIVED) {
                        FilledTonalButton(
                            onClick = { onNavigate(Screen.ArchivedThreads) },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .animateItem()
                                .selfAlignHorizontally()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.archived_outlined),
                                contentDescription = null
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.archived))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                threadsList(
                    pinnedThreads = state.pinnedConversations,
                    threads = state.conversations,
                    sweetSelectState = sweetSelectState,
                    onNavigate = onNavigate,
                    sharedTransitionScope = this@ConversationsScreen,
                    onHandleConversationsAction = onHandleConversationsAction,
                    emptyState = {
                        NoXFound(
                            headlineText = R.string.no_convo_found,
                            bodyText = R.string.no_convo_found_desc,
                            icon = R.drawable.message_rounded
                        )
                    }
                )
            }
        }
    }
}

fun LazyListScope.threadsList(
    pinnedThreads: List<CuteConversation>,
    threads: List<CuteConversation>,
    sweetSelectState: SweetSelectState<CuteConversation>,
    onNavigate: (Screen) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    emptyState: @Composable () -> Unit,
    onHandleConversationsAction: (ConversationsAction) -> Unit
) {
    item(LazyListKeys.PINNED_CONVERSATIONS) {
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(
                items = pinnedThreads,
                key = { it.threadId }
            ) { conversation ->

                val isSelected by remember {
                    derivedStateOf { sweetSelectState.isSelected(conversation) }
                }

                PinnedConversation(
                    cuteConversation = conversation,
                    isSelected = isSelected,
                    onClick = {
                        if (sweetSelectState.isInSelectionMode) {
                            sweetSelectState.toggle(conversation)
                        } else {
                            onNavigate(Screen.Conversation(conversation.threadId))
                        }
                    },
                    onLongClick = { sweetSelectState.toggle(conversation) }
                )
            }
        }
    }

    with(sharedTransitionScope) {
        items(
            items = threads,
            key = { conversation -> conversation.threadId }
        ) { conversation ->


            val isSelected by remember {
                derivedStateOf { sweetSelectState.isSelected(conversation) }
            }

            Conversation(
                conversation = conversation,
                modifier = Modifier.animateItem(),
                onClick = {
                    if (sweetSelectState.isInSelectionMode) {
                        sweetSelectState.toggle(conversation)
                    } else {
                        onNavigate(Screen.Conversation(conversation.threadId))
                    }
                },
                onLongClick = { sweetSelectState.toggle(conversation) },
                isSelected = isSelected,
                onHandleConversationsAction = onHandleConversationsAction
            )
        }
    }
    if (threads.isEmpty() && pinnedThreads.isEmpty()) {
        item { emptyState() }
    }
}


