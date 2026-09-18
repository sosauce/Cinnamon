@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.voicemail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.MoreOptions
import com.sosauce.cinnamon.features.phone.domain.CuteVoicemail
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.components.AnimatedSelectedIcon

@Composable
fun VoicemailItem(
    modifier: Modifier = Modifier,
    voicemail: CuteVoicemail,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isPlaying: Boolean,
    isSelected: Boolean,
    onPlayPause: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onDelete: () -> Unit
) {

    val context = LocalContext.current
    var showMoreOptions by remember { mutableStateOf(false) }

    val moreOptions = listOf(
        MoreOptions(
            onClick = {
                val threadId = voicemail.number.getThreadIdOrCreate(context)
                onNavigate(Screen.ConversationDetails(threadId))
            },
            icon = R.drawable.message_rounded,
            text = R.string.send_msg
        ),
        MoreOptions(
            onClick = onDelete,
            icon = R.drawable.delete,
            text = R.string.delete,
            tint = MaterialTheme.colorScheme.error
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f
    )


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
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(MaterialShapes.Circle.toShape())
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {

                    val firstChar = voicemail.displayName.firstOrNull() ?: '?'

                    if (firstChar.isLetter()) {
                        Text(
                            text = firstChar.uppercase(),
                            style = MaterialTheme.typography.titleLargeEmphasized.copy(
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    AsyncImage(
                        model = voicemail.photo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        trailingContent = {
            IconButton(
                onClick = onPlayPause,
                shapes = IconButtonDefaults.shapes()
            ) {
                AnimatedDrawable(
                    drawable = AnimatedDrawableFile.PLAY,
                    atEnd = isPlaying
                )
            }
            IconButton(
                onClick = { showMoreOptions = !showMoreOptions },
                shapes = IconButtonDefaults.shapes()
            ) {
                AnimatedDrawable(
                    drawable = AnimatedDrawableFile.MORE_VERT,
                    atEnd = showMoreOptions
                )
            }
            DropdownMenuPopup(
                expanded = showMoreOptions,
                onDismissRequest = { showMoreOptions = false }
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes()
                ) {
                    moreOptions.fastForEachIndexed { index, option ->
                        DropdownMenuItem(
                            onClick = {
                                option.onClick()
                                showMoreOptions = false
                            },
                            shape = MenuDefaults.getItemShape(index, moreOptions.lastIndex),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(option.icon),
                                    contentDescription = null,
                                    tint = option.tint ?: LocalContentColor.current
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(option.text),
                                    color = option.tint ?: LocalContentColor.current
                                )
                            }
                        )
                    }
                }
            }
        }
    ) {
        Text(
            text = voicemail.displayName
        )
        Text(
            text = "${voicemail.date} · ${voicemail.duration}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}