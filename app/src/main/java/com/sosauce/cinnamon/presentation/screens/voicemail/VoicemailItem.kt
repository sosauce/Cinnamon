package com.sosauce.cinnamon.presentation.screens.voicemail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteVoicemail
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.messages.components.bottombar.MoreOptions
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.animations.AnimatedMoreIcon
import com.sosauce.cinnamon.presentation.shared_components.animations.AnimatedSelectedIcon
import com.sosauce.cinnamon.presentation.shared_components.items.CuteListItem
import com.sosauce.cinnamon.utils.getItemShape
import com.sosauce.cinnamon.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.utils.toDate
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
                onNavigate(Screen.Conversation(threadId))
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
                DefaultContactIcon(
                    firstLetter = voicemail.displayName.firstOrNull(),
                    contactPhoneNumber = voicemail.number
                )
            }
        },
        trailingContent = {
            IconButton(
                onClick = onPlayPause,
                shapes = IconButtonDefaults.shapes()
            ) {
                val icon = if (isPlaying) {
                    R.drawable.pause
                } else R.drawable.play

                Icon(
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }
            IconButton(
                onClick = { showMoreOptions = !showMoreOptions },
                shapes = IconButtonDefaults.shapes()
            ) {
                AnimatedMoreIcon(
                    expanded = showMoreOptions
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
        Text(voicemail.displayName)
        Text(
            text = "${voicemail.date.toDate()} · ${voicemail.duration.toDuration(DurationUnit.SECONDS)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}