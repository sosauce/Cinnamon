@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.messages.components

import android.net.Uri
import android.provider.BlockedNumberContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.domain.model.ConversationSettings
import com.sosauce.cuteconnect.domain.model.CuteContact
import com.sosauce.cuteconnect.domain.model.CuteConversation
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.ui.shared_components.CuteDropdownMenuItem
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import com.sosauce.cuteconnect.ui.shared_components.DefaultGroupChatIcon
import com.sosauce.cuteconnect.utils.betterFormatNumber
import com.sosauce.cuteconnect.utils.getContactNameOrNothing
import com.sosauce.cuteconnect.utils.getContactPfpUri
import com.sosauce.cuteconnect.utils.toDate
import com.sosauce.cuteconnect.utils.toShortDate

@Composable
fun Conversation(
    modifier: Modifier = Modifier,
    cuteConversation: CuteConversation,
    cuteContact: CuteContact?,
    conversationSettings: ConversationSettings,
    onClick: (Screen) -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val nameOrNumber = remember(cuteConversation.recipients) {
        cuteConversation.recipients.map { it.getContactNameOrNothing(context).betterFormatNumber() }
    }
    var showUnblockDialog by remember { mutableStateOf(false) }



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
                    CuteText(
                        text = stringResource(R.string.unblock)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnblockDialog = false }
                ) {
                    CuteText(
                        text = stringResource(R.string.cancel)
                    )
                }
            },
            text = {
                CuteText(
                    text = stringResource(
                        R.string.unblock_no_u_sure,
                        cuteConversation.recipients.first()
                    )
                )
            },
            title = {
                CuteText(
                    text = stringResource(R.string.unblock_no)
                )
            }
        )
    }



    CuteDropdownMenuItem(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        onClick = { onClick(Screen.Conversation(cuteConversation.recipients.first())) },
        onLongClick = onLongClick,
        leadingIcon = {
            if (cuteConversation.isGroupChat) {
                DefaultGroupChatIcon()
            } else {
                DefaultContactIcon(
                    firstLetter = nameOrNumber.firstOrNull()?.firstOrNull(),
                    contactPfp = cuteContact?.photo ?: Uri.EMPTY
                )
            }
        },
        trailingIcon = {
            Column(
                horizontalAlignment = Alignment.End
            ) {

                CuteText(
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
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialShapes.Circle.toShape()
                                )
                                .size(10.dp)
                        )
                    }

                    if (cuteConversation.isSenderBlocked) {
                        IconButton(
                            onClick = { showUnblockDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 15.dp)
            ) {
                CuteText(
                    text = buildString {
                        nameOrNumber.fastForEachIndexed { index, text ->
                            append(text)
                            if (index != nameOrNumber.lastIndex) {
                                append(", ")
                            }
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CuteText(
                    text = if (cuteConversation.isSenderBlocked) {
                        stringResource(R.string.you_blocked_this_no)
                    } else {
                        if (conversationSettings.draft.isNotEmpty()) {
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Draft:") }
                                append(" ")
                                append(conversationSettings.draft)
                            }.text
                        } else {
                            cuteConversation.snippet
                        }
                    },
                    maxLines = if (cuteConversation.read) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontStyle = if (cuteConversation.isSenderBlocked) FontStyle.Italic else FontStyle.Normal,
                        color = if (cuteConversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    )
                )
            }
        }
    )
}





