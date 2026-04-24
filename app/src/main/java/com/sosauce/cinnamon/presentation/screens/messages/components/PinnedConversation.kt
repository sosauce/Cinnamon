@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.DefaultGroupChatIcon
import com.sosauce.cinnamon.presentation.shared_components.SelectedItemLogo
import com.sosauce.cinnamon.utils.getContactId
import com.sosauce.cinnamon.utils.getContactPfpUriFromId
import com.sosauce.cinnamon.utils.thenIf

@Composable
fun PinnedConversation(
    modifier: Modifier = Modifier,
    cuteConversation: CuteConversation,
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
            .thenIf(!cuteConversation.read) {
                border(width = 2.dp, color = primary, RoundedCornerShape(10.dp))
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = { scaleIn() togetherWith scaleOut() }
            ) {
                if (it) {
                    SelectedItemLogo(size = 70.dp)
                } else {
                    if (cuteConversation.isGroupChat) {
                        DefaultGroupChatIcon(size = 70.dp)
                    } else {
                        DefaultContactIcon(
                            firstLetter = cuteConversation.recipients.first().firstOrNull(),
                            size = 70.dp,
                            contactPfp = cuteConversation.rawRecipients.first().getContactId(context).getContactPfpUriFromId()
                        )
                    }
                }
            }
            Text(
                text = cuteConversation.recipients.first(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cuteConversation.snippet,
                maxLines = if (cuteConversation.read) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                    fontStyle = if (cuteConversation.isSenderBlocked) FontStyle.Italic else FontStyle.Normal,
                    color = if (cuteConversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    }
}