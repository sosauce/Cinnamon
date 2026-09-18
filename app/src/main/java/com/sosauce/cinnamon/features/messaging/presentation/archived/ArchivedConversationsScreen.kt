@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.archived

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.ConversationsSelectedBar
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.core.ui.components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationsAction
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.dialogs.DeleteConversationsDialog
import com.sosauce.cinnamon.features.messaging.presentation.conversation.threadsList
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.sweetselect.rememberSweetSelectState

@Composable
fun SharedTransitionScope.ArchivedConversationsScreen(
    state: ArchivedState,
    onNavigate: (Screen) -> Unit,
    onNavigateUp: () -> Unit,
    onHandleThreadsAction: (ConversationsAction) -> Unit
) {

    val sweetSelectState = rememberSweetSelectState<CuteConversation>()
    var showDeleteConversationsDialog by remember { mutableStateOf(false) }

    if (showDeleteConversationsDialog) {
        DeleteConversationsDialog(
            onDismissRequest = { showDeleteConversationsDialog = false },
            onDelete = {
                val threadIds = sweetSelectState.selectedItems.map { it.threadId }
                onHandleThreadsAction(ConversationsAction.DeleteConversations(threadIds))

                showDeleteConversationsDialog = false
            },
            numberOfConversations = sweetSelectState.selectedItems.size
        )
    }



    Scaffold(
        bottomBar = {
            AnimatedContent(
                targetState = sweetSelectState.isInSelectionMode,
            ) {
                if (!it) {
                    CuteSearchbar(
                        modifier = Modifier.selfAlignHorizontally(),
                        onNavigate = onNavigate,
                        navigationIcon = { CuteNavigationButtonSurface(onNavigateUp = onNavigateUp) }
                    )
                } else {
                    ConversationsSelectedBar(
                        modifier = Modifier.selfAlignHorizontally(),
                        items = state.conversations,
                        multiSelectState = sweetSelectState,
                        onDeleteConversations = {
                            val threadIds = sweetSelectState.selectedItems.map { it.threadId }
                            onHandleThreadsAction(ConversationsAction.DeleteConversations(threadIds))
                            sweetSelectState.clearSelected()
                        },
                        onArchiveThreads = {
                            val threadIds = sweetSelectState.selectedItems.map { it.threadId }
                            onHandleThreadsAction(ConversationsAction.ArchiveConversations(threadIds))
                            sweetSelectState.clearSelected()
                        },
                        onPinThreads = {
                            val threadIds = sweetSelectState.selectedItems.map { it.threadId }
                            onHandleThreadsAction(ConversationsAction.PinConversations(threadIds))
                            sweetSelectState.clearSelected()
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues
        ) {
            threadsList(
                pinnedConversations = emptyList(),
                conversations = state.conversations,
                sweetSelectState = sweetSelectState,
                onNavigate = onNavigate,
                sharedTransitionScope = this@ArchivedConversationsScreen,
                emptyState = {
                    NoXFound(
                        headlineText = R.string.no_archived_convos,
                        bodyText = R.string.no_archived_convos_desc,
                        icon = R.drawable.archived_outlined
                    )
                },
                onHandleConversationsAction = onHandleThreadsAction
            )

        }

    }
}