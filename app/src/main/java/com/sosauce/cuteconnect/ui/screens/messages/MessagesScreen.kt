@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sosauce.cuteconnect.domain.model.CuteConversation
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.ui.screens.messages.components.Conversation
import com.sosauce.cuteconnect.ui.shared_components.searchbars.CuteSearchbar
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.domain.model.ConversationSettings
import com.sosauce.cuteconnect.ui.shared_components.SelectedBar
import com.sosauce.cuteconnect.ui.theme.CuteConnectTheme
import com.sosauce.cuteconnect.utils.LocalScreen
import com.sosauce.cuteconnect.utils.addOrRemove
import com.sosauce.cuteconnect.utils.rememberSearchbarAlignment
import com.sosauce.cuteconnect.utils.showCuteSearchbar

@Composable
fun MessagesScreen(
    conversations: List<CuteConversation>,
    onNavigate: (Screen) -> Unit,
    onEditPinnedConvos: (String) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    //val conversationViewModel = koinViewModel<ConversationViewModel>()
    //val pinnedConvos by conversationViewModel.getPinnedConversations().collectAsStateWithLifecycle()

    val selectedConversations = remember { mutableStateListOf<CuteConversation>() }
    Scaffold(
        bottomBar = {
            AnimatedContent(
                targetState = selectedConversations.isEmpty(),
                transitionSpec = { scaleIn() togetherWith scaleOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth()
            ) {
                if (it) {
                    CuteSearchbar(
                        sortingMenu = {},
                        fab = {
                            SmallFloatingActionButton(
                                onClick = { onNavigate(Screen.StartConversation) },
                                shape = MaterialShapes.Cookie7Sided.toShape()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null
                                )
                            }
                        },
                        onNavigate = onNavigate
                    )
                } else {
                    SelectedBar(
                        numberOfSelectedElements = selectedConversations.size,
                        onClearSelected = selectedConversations::clear
                    ) {
                        IconButton(
                            onClick = {
                                selectedConversations.fastForEach {
//                                        scope.launch {
//                                            val convoSettings = conversationViewModel.getConversationSettings(it.threadId).first() ?: ConversationSettings(it.threadId) // Avoid having to always copy the Id
//
//                                            conversationViewModel.handleConversationSettingsActions(
//                                                ConversationSettingActions.UpsertConversationSettings(
//                                                    convoSettings.copy(
//                                                        convoId = it.threadId,
//                                                        isPinned = true
//                                                    )
//                                                )
//                                            )
//                                        }
                                }
                                selectedConversations.clear()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.pin_filled),
                                contentDescription = "pin conversations"
                            )
                        }
                        IconButton(
                            onClick = { /* archive selected convos */ }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.archive),
                                contentDescription = "archive conversations"
                            )
                        }
                        IconButton(
                            onClick = { /* have a confirmation dialog */ }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.delete_filled),
                                contentDescription = "delete conversations",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
//            item {
//                Row(Modifier.fillMaxWidth().clickable { onNavigate(Screen.DebugMms) }.statusBarsPadding()) {
//                    CuteText("All Mms Debug")
//                }
//            }
//                item("pinned convos") {
//                    LazyRow {
//                        items(
//                            items = allConversations.fastFilter { it.threadId in pinnedConvos },
//                            key = { it.threadId }
//                        ) { cuteConversation ->
//                            PinnedConversation(
//                                cuteConversation = cuteConversation,
//                                onNavigate = { screen ->
//                                    if (selectedConversations.isEmpty()) {
//                                        onNavigate(screen)
//                                    } else {
//                                        selectedConversations.addOrRemove(cuteConversation)
//                                    }
//                                },
//                                onLongClick = { selectedConversations.addOrRemove(cuteConversation) },
//                                isSelected = selectedConversations.contains(cuteConversation)
//                            )
//                        }
//                    }
//                }
            items(
                items = conversations,
                key = { it.threadId }
            ) { cuteConversation ->
                //val conversationSettings by conversationViewModel.getConversationSettings(cuteConversation.threadId).collectAsStateWithLifecycle(ConversationSettings())

                Conversation(
                    cuteConversation = cuteConversation,
                    conversationSettings = ConversationSettings(),
                    cuteContact = cuteConversation.contacts.firstOrNull(), // If user has multiple contacts with same number, then not our problem why would you do that anyways!
                    modifier = Modifier
                        .animateItem()
                        .padding(
                            vertical = 4.dp,
                            horizontal = 2.dp
                        )
                        .background(
                            color = if (selectedConversations.contains(cuteConversation)) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    onClick = { screen ->
                        if (selectedConversations.isEmpty()) {
                            onNavigate(screen)
                        } else {
                            selectedConversations.addOrRemove(cuteConversation)
                        }
                    },
                    onLongClick = { selectedConversations.addOrRemove(cuteConversation) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun MessagesScreenPreview() {

    CuteConnectTheme {
        CompositionLocalProvider(LocalScreen provides Screen.Messages) {

            MessagesScreen(
                conversations = emptyList(),
                onNavigate = {}
            ) { }
        }
    }

}