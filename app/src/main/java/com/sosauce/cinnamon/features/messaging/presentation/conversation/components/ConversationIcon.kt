package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation


@Composable
fun ConversationIcon(
    conversation: CuteConversation,
    size: Dp = 50.dp,
    highQuality: Boolean = false
) {
    DefaultContactIcon(
        firstLetter = conversation.name.firstOrNull(),
        size = size,
        contactPfp = if (highQuality) conversation.hqPhoto else conversation.photo
    )
}
