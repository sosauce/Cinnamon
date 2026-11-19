package com.sosauce.cinnamon.features.messaging.presentation.conversation.forwarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.core.ui.components.searchbars.SimpleSearchbar
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.Conversation
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.components.LoadingBox

@Composable
fun ForwardingScreen(
    state: ForwardingState,
    textFieldState: TextFieldState,
    onNavigate: (Screen) -> Unit,
    onNavigateBack: () -> Unit
) {
    LoadingBox(
        isLoading = state.isLoading
    ) {
        Scaffold(
            bottomBar = {
                SimpleSearchbar(
                    modifier = Modifier.selfAlignHorizontally(),
                    textFieldState = textFieldState,
                    placeholder = R.string.type_name_number,
                    navigationButton = {
                        AnimatedFab(
                            onClick = onNavigateBack,
                            icon = R.drawable.back,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
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
                        modifier = Modifier.animateItem(),
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