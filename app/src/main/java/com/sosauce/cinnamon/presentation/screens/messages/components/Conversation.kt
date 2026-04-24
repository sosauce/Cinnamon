@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.DefaultGroupChatIcon
import com.sosauce.cinnamon.presentation.shared_components.SelectedItemLogo
import com.sosauce.cinnamon.utils.getContactId
import com.sosauce.cinnamon.utils.getContactPfpUriFromId
import com.sosauce.cinnamon.utils.toDate

@Composable
fun Conversation(
    modifier: Modifier = Modifier,
    cuteConversation: CuteConversation,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    shape: Shape = RoundedCornerShape(24.dp)
) {
    val context = LocalContext.current
    var showUnblockDialog by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f
    )



    if (showUnblockDialog) {
        AlertDialog(
            onDismissRequest = { showUnblockDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        //BlockedNumberContract.unblock(context, cuteConversation.recipients.first())
                        showUnblockDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.unblock)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnblockDialog = false }
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
                        cuteConversation.recipients.first()
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

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .padding(3.dp)
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = modifier
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = { scaleIn() togetherWith scaleOut() },
                modifier = Modifier.padding(start = 10.dp)
            ) {
                if (it) {
                    SelectedItemLogo()
                } else {
                    if (cuteConversation.isGroupChat) {
                        DefaultGroupChatIcon()
                    } else {
                        DefaultContactIcon(
                            firstLetter = cuteConversation.recipients.firstOrNull()?.firstOrNull(),
                            contactPfp = cuteConversation.rawRecipients.firstOrNull()?.getContactId(context)?.getContactPfpUriFromId() ?: Uri.EMPTY
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = buildString {
                        cuteConversation.recipients.fastForEachIndexed { index, text ->
                            append(text)
                            if (index != cuteConversation.recipients.lastIndex) {
                                append(", ")
                            }
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val text = when {
                    cuteConversation.isSenderBlocked -> {
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
                    cuteConversation.draft.isNotEmpty() -> {
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontStyle = FontStyle.Italic
                                )
                            ) { append("Draft:") }
                            append(" ")
                            append(cuteConversation.draft)
                        }
                    }
                    else -> AnnotatedString(cuteConversation.snippet)
                }


                AnimatedContent(text) {
                    Text(
                        text = it,
                        maxLines = if (cuteConversation.read) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            color = if (cuteConversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                        )
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 5.dp)
            ) {
                Text(
                    text = cuteConversation.date.toDate(),
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = if (cuteConversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (!cuteConversation.read) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            // Text("99+")
                        }
                    }

                    if (cuteConversation.isSenderBlocked) {
                        IconButton(
                            onClick = { showUnblockDialog = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.block),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }

    }

}





