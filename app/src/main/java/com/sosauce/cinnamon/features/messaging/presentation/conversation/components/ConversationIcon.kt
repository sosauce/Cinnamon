package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.core.ui.components.CuteAvatar
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation

@Composable
fun ConversationIcon(
    conversation: CuteConversation,
    size: Dp = 50.dp
) {
    CuteAvatar(
        name = conversation.name,
        photo = conversation.photo,
        size = size
    )
}
