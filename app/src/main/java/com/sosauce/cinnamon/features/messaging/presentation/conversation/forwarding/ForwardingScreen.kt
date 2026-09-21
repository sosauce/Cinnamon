package com.sosauce.cinnamon.features.messaging.presentation.conversation.forwarding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.Conversation
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.helpers.ScopedViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ForwardingScreen(
    state: ForwardingState,
    onNavigate: (Screen) -> Unit,
    onNavigateBack: () -> Unit
) {
    LoadingBox(
        isLoading = state.isLoading
    ) {
        Scaffold(
            bottomBar = {
                CuteSearchbar(
                    modifier = Modifier.selfAlignHorizontally(),
                    navigationIcon = {
                        AnimatedFab(
                            onClick = onNavigateBack,
                            icon = R.drawable.back,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    },
                    onNavigate = onNavigate
                )
            }
        ) { pv ->
            LazyColumn(
                contentPadding = pv
            ) {
                items(
                    items = state.conversations,
                    key = { it.threadId }
                ) { conversation ->
                    Conversation(
                        onClick = {
                            println("forward, clicking convo: ${state.messageToForward}")
                            onNavigate(
                                Screen.ConversationDetails(
                                    threadId = conversation.threadId,
                                    prefilledMessage = state.messageToForward
                                )
                            )
                        },
                        conversation = conversation
                    )
                }
            }
        }
    }

}