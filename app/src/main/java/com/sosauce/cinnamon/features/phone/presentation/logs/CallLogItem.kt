@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.logs

import android.content.ClipData
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.menus.MoreOptionsMenu
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.MoreOptions
import com.sosauce.cinnamon.features.phone.domain.CallPresentation
import com.sosauce.cinnamon.features.phone.domain.CallType
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.components.AnimatedSelectedIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun CallLogItem(
    modifier: Modifier = Modifier,
    callLog: CuteCallLog,
    isSelected: Boolean,
    numberOfAppearance: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallLogsAction: (CallLogsAction) -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteCallLog: () -> Unit
) {


    val context = LocalContext.current
    val moreOptionsDescription = stringResource(R.string.more_options)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showMoreOptions by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f
    )


    val icon = when (callLog.callType) {
        CallType.INCOMING -> R.drawable.arrow_315
        CallType.OUTGOING -> R.drawable.arrow_45
        CallType.MISSED -> R.drawable.call_missed
        CallType.REJECTED -> R.drawable.block
    }

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val iconColor = when (callLog.callType) {
        CallType.INCOMING, CallType.OUTGOING -> if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
        CallType.MISSED -> MaterialTheme.colorScheme.error
        CallType.REJECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }


    val actions = buildList {
        if (callLog.presentation == CallPresentation.ALLOWED) {
            add(
                MoreOptions(
                    onClick = {
                        onCallLogsAction(CallLogsAction.CallNumber(callLog.number))
                    },
                    icon = R.drawable.phone,
                    text = R.string.call
                )
            )
            add(
                MoreOptions(
                    onClick = {
                        scope.launch {
                            val threadId = withContext(Dispatchers.IO) {
                                callLog.number.getThreadIdOrCreate(context)
                            }
                            onNavigate(Screen.ConversationDetails(threadId))
                        }
                    },
                    icon = R.drawable.message_rounded,
                    text = R.string.send_msg
                )
            )
            add(
                MoreOptions(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(callLog.number, callLog.number)
                                )
                            )
                            Toast.makeText(context, R.string.number_copied, Toast.LENGTH_SHORT).show()
                        }
                    },
                    icon = R.drawable.copy,
                    text = R.string.copy_number
                )
            )
            if (!callLog.hasContactAssociated) {
                add(
                    index = 1,
                    element = MoreOptions(
                        onClick = {
                            onNavigate(
                                Screen.ContactEditor(
                                    rawContactId = null,
                                    prefilledNumber = callLog.number
                                )
                            )
                        },
                        icon = R.drawable.add_contact,
                        text = R.string.add_contact
                    )
                )
            }
        }
        add(
            MoreOptions(
                onClick = onDeleteCallLog,
                text = R.string.delete,
                icon = R.drawable.delete,
                tint = MaterialTheme.colorScheme.error
            )
        )
    }


    CuteListItem(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            AnimatedSelectedIcon(
                isSelected = isSelected
            ) {
                DefaultContactIcon(
                    firstLetter = callLog.displayName.firstOrNull(),
                    contactPfp = callLog.photo
                )
            }
        },
        trailingContent = {
            Row {
                IconButton(
                    onClick = { showMoreOptions = true },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.semantics {
                        contentDescription = moreOptionsDescription
                    }
                ) {
                    AnimatedDrawable(
                        drawable = AnimatedDrawableFile.MORE_VERT,
                        atEnd = showMoreOptions
                    )
                }

                MoreOptionsMenu(
                    expanded = showMoreOptions,
                    onDismissRequest = { showMoreOptions = false },
                    options = actions
                )
            }
        }
    ) {
        Text(
            text = if (numberOfAppearance <= 1) callLog.displayName else "${callLog.displayName} ($numberOfAppearance)",
            style = MaterialTheme.typography.titleMediumEmphasized,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = buildString {
                        append(callLog.time)
                        if (callLog.duration != null && (callLog.callType == CallType.INCOMING || callLog.callType == CallType.OUTGOING)) {
                            append(" · ")
                            append(callLog.duration)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(),
                    style = MaterialTheme.typography.bodyMediumEmphasized
                )
            }
            callLog.location?.let { location ->
                Spacer(Modifier.width(2.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        callLog.sim?.let { sim ->
            val simColor = sim.color?.let { Color(it) } ?: LocalContentColor.current
            val contentColor = if (simColor.luminance() > 0.5f) Color.Black else Color.White

            Row(
                modifier = Modifier
                    .padding(
                        vertical = 5.dp
                    )
                    .background(
                        color = simColor,
                        shape = CircleShape
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.sim_card),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = sim.name,
                    style = MaterialTheme.typography.labelMediumEmphasized.copy(
                        color = contentColor
                    ),
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}
