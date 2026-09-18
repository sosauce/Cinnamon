@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.logs

import android.content.ClipData
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
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
import com.sosauce.cinnamon.features.phone.domain.CallPresentation
import com.sosauce.cinnamon.features.phone.domain.CallType
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.components.AnimatedSelectedIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun CallLogItem(
    modifier: Modifier = Modifier,
    callLog: CuteCallLog2,
    isSelected: Boolean,
    numberOfAppearance: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallAction: (CallAction) -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteCallLog: () -> Unit
) {


    val context = LocalContext.current
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


    val actions = buildList {
        if (callLog.presentation == CallPresentation.ALLOWED) {
            add(
                CallLogAction(
                    onClick = {
                        onCallAction(CallAction.LaunchCall(callLog.number))
                        showMoreOptions = false
                    },
                    icon = R.drawable.phone,
                    text = R.string.call
                )
            )
            add(
                CallLogAction(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val threadId = callLog.number.getThreadIdOrCreate(context)
                            onNavigate(Screen.ConversationDetails(threadId))
                            showMoreOptions = false
                        }
                    },
                    icon = R.drawable.message_rounded,
                    text = R.string.send_msg
                )
            )
            add(
                CallLogAction(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(callLog.number, callLog.number)
                                )
                            )
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            showMoreOptions = false
                        }
                    },
                    icon = R.drawable.copy,
                    text = R.string.copy_number
                )
            )
        }
        add(
            CallLogAction(
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
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(MaterialShapes.Circle.toShape())
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {

                    val firstChar = callLog.displayName.firstOrNull() ?: '?'

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
                        model = callLog.photo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(
                    onClick = { showMoreOptions = true },
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
                        actions.fastForEachIndexed { index, action ->
                            DropdownMenuItem(
                                onClick = action.onClick,
                                shape = MenuDefaults.getItemShape(index, actions.lastIndex),
                                text = {
                                    Text(
                                        text = stringResource(action.text),
                                        color = action.tint ?: LocalContentColor.current
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(action.icon),
                                        contentDescription = null,
                                        tint = action.tint ?: LocalContentColor.current
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Text(
            text = if (numberOfAppearance <= 1) callLog.displayName else "${callLog.displayName} ($numberOfAppearance)",
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val providedColor =
                if (callLog.callType == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            CompositionLocalProvider(LocalContentColor provides providedColor) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
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
                    modifier = Modifier.basicMarquee(),
                    style = MaterialTheme.typography.bodyMediumEmphasized
                )
            }
            callLog.location?.let { location ->
                Spacer(Modifier.weight(1f))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

private data class CallLogAction(
    val onClick: () -> Unit,
    val icon: Int,
    val text: Int,
    val tint: Color? = null
)