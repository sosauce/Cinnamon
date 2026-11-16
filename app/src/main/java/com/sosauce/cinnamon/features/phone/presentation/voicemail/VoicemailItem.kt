@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.voicemail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.menus.MoreOptionsMenu
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
                DefaultContactIcon(
                    firstLetter = voicemail.displayName.firstOrNull(),
                    contactPfp = voicemail.photo
                )
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
            MoreOptionsMenu(
                expanded = showMoreOptions,
                onDismissRequest = { showMoreOptions = false },
                options = moreOptions
            )
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
