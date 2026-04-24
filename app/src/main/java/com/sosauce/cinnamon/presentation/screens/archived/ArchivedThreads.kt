@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.archived

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.messages.ConversationsAction
import com.sosauce.cinnamon.presentation.screens.messages.components.dialogs.DeleteConversationsDialog
import com.sosauce.cinnamon.presentation.screens.messages.threadsList
import com.sosauce.cinnamon.presentation.shared_components.ConversationsSelectedBar
import com.sosauce.cinnamon.presentation.shared_components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import com.sosauce.sweetselect.rememberSweetSelectState

@Composable
fun ArchivedThreads(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(),
                        sortingMenu = {},
                        fab = {
                            FloatingActionButton(
                                onClick = { onNavigate(Screen.StartConversation) },
                                shape = MaterialShapes.Cookie9Sided.toShape()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null
                                )
                            }
                        },
                        onNavigate = onNavigate
                    )
                } else {
                    ConversationsSelectedBar(
                        modifier = Modifier.selfAlignHorizontally(),
                        items = state.threads,
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
        key(state.threads) {
            LazyColumn(
                contentPadding = paddingValues
            ) {
                threadsList(
                    pinnedThreads = emptyList(),
                    threads = state.threads,
                    sweetSelectState = sweetSelectState,
                    onNavigate = onNavigate
                )

            }
        }

    }
}