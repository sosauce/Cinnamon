@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import android.provider.BlockedNumberContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.DefaultGroupChatIcon
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.nekobites.components.AnimatedSelectedIcon

@Composable
fun Conversation(
    modifier: Modifier = Modifier,
    conversation: CuteConversation,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false
) {
    val context = LocalContext.current
    var showUnblockDialog by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f
    )



    if (showUnblockDialog) {

        // Can only block individual chats, not group chats
        val blockedParticipant = conversation.participants.firstOrNull() ?: return

        AlertDialog(
            onDismissRequest = { showUnblockDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.block),
                    contentDescription = null
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        BlockedNumberContract.unblock(
                            context,
                            blockedParticipant.rawNumber
                        )
                        showUnblockDialog = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = stringResource(R.string.unblock)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnblockDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = stringResource(R.string.cancel)
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.unblock_no_u_sure,
                        blockedParticipant.displayName
                    )
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.unblock_no)
                )
            }
        )
    }

    CuteListItem(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        leadingContent = {
            AnimatedSelectedIcon(
                isSelected = isSelected
            ) {
                if (conversation.isGroupChat) {
                    DefaultGroupChatIcon()
                } else {
                    ConversationIcon(conversation)
                }
            }
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 5.dp)
            ) {

                if (conversation.isAnyBlocked && !conversation.isGroupChat) {
                    IconButton(
                        onClick = { showUnblockDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.block),
                            contentDescription = null,
                        )
                    }
                } else {
                    Text(
                        text = conversation.date,
                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                            color = if (conversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                        )
                    )
                    if (!conversation.read) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            // Text("99+")
                        }
                    }
                }


            }
        }
    ) {
        Text(
            text = conversation.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val text = when {
            conversation.isAnyBlocked && !conversation.isGroupChat -> {
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(stringResource(R.string.you_blocked_this_no))
                    }
                }
            }

            conversation.draft.isNotEmpty() -> {
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = FontStyle.Italic
                        )
                    ) { append("Draft:") }
                    append(" ")
                    append(conversation.draft)
                }
            }

            else -> AnnotatedString(conversation.snippet)
        }


        AnimatedContent(text) {
            Text(
                text = it,
                maxLines = if (conversation.read) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                    color = if (conversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    }

}





