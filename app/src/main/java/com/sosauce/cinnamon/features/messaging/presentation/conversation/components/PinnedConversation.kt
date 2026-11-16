@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.chocola.core.designsystem.components.FavoriteBubble
import com.sosauce.chocola.core.designsystem.components.FavoriteItem
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation

@Composable
fun PinnedConversation(
    modifier: Modifier = Modifier,
    conversation: CuteConversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isInSelectionMode: Boolean = false
) {

    val badge: (@Composable () -> Unit)? = if (conversation.unreadCount > 0) {
        {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.defaultMinSize(25.dp, 25.dp)
            ) {
                Text(
                    text = if (conversation.unreadCount > 99) {
                        stringResource(R.string.unread_count_overflow)
                    } else {
                        conversation.unreadCount.toString()
                    }
                )
            }
        }
    } else null

    FavoriteItem(
        modifier = modifier,
        title = conversation.name,
        subtitle = conversation.snippet,
        isSelected = isSelected,
        isInSelectionMode = isInSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
        favoriteBubble = {
            FavoriteBubble(
                icon = R.drawable.pin_filled
            )
        },
        visual = {
            ConversationIcon(
                conversation = conversation,
                size = 90.dp,
                highQuality = true
            )
        },
        extraBadge = badge
    )
}


