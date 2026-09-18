@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.core.utils.thenIf
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.nekobites.components.AnimatedSelectedIcon

@Composable
fun PinnedConversation(
    modifier: Modifier = Modifier,
    conversation: CuteConversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean
) {


    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .widthIn(max = 150.dp)
            .padding(10.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .thenIf(!conversation.read) {
                border(width = 2.dp, color = primary, RoundedCornerShape(10.dp))
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedSelectedIcon(
                isSelected = isSelected
            ) {
                ConversationIcon(conversation)
            }
            Text(
                text = conversation.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLargeEmphasized
            )
            Text(
                text = conversation.snippet,
                maxLines = if (conversation.read) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                    fontStyle = if (conversation.isAnyBlocked && !conversation.isGroupChat) FontStyle.Italic else FontStyle.Normal,
                    color = if (conversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    }
}