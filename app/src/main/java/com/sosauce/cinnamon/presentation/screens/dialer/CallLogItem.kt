@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.dialer

import android.content.ClipData
import android.provider.CallLog
import android.widget.Toast
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteCallLog
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.items.CuteListItem
import com.sosauce.cinnamon.utils.beautifyNumber
import com.sosauce.cinnamon.utils.getItemShape
import com.sosauce.cinnamon.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.utils.secondsToDuration
import com.sosauce.cinnamon.utils.toTime
import com.sosauce.sweetselect.SweetSelectState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun CallLogItem(
    modifier: Modifier = Modifier,
    callLog: CuteCallLog,
    numberOfAppearance: Int,
    onCallAction: (CallAction) -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteCallLog: () -> Unit,
    sweetSelectState: SweetSelectState<CuteCallLog>
) {


    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showMoreOptions by remember { mutableStateOf(false) }
    val displayNameOrNumber = callLog.cachedName ?: callLog.rawNumber.beautifyNumber()


    val icon = when(callLog.callType) {
        CallLog.Calls.INCOMING_TYPE -> R.drawable.arrow_315
        CallLog.Calls.OUTGOING_TYPE -> R.drawable.arrow_45
        CallLog.Calls.MISSED_TYPE -> R.drawable.call_missed
        CallLog.Calls.REJECTED_TYPE -> R.drawable.block
        else -> R.drawable.block
    }


    val actions = buildList {
        if (callLog.presentation == CallLog.Calls.PRESENTATION_ALLOWED) {
            add(
                CallLogAction(
                    onClick = {
                        onCallAction(CallAction.LaunchCall(callLog.rawNumber))
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
                            val threadId = callLog.rawNumber.getThreadIdOrCreate(context)
                            onNavigate(Screen.Conversation(threadId))
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
                                    ClipData.newPlainText(callLog.rawNumber, callLog.rawNumber)
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
        modifier = modifier,
        onClick = if (callLog.presentation == CallLog.Calls.PRESENTATION_ALLOWED) {
            // TODO multi select
            { onCallAction(CallAction.LaunchCall(callLog.rawNumber)) }
        } else null,
        leadingContent = {
            DefaultContactIcon(
                firstLetter = displayNameOrNumber.firstOrNull(),
                modifier = Modifier.padding(start = 10.dp),
                contactPfp = callLog.cachedPicture
            )
        },
        trailingContent = {
            Row {
                IconButton(
                    onClick = { showMoreOptions = true },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
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
            text = if (numberOfAppearance <= 1) displayNameOrNumber else "$displayNameOrNumber ($numberOfAppearance)",
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val providedColor = if (callLog.callType == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            CompositionLocalProvider(LocalContentColor provides providedColor) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = buildString {
                        append(callLog.date.toTime())
                        if (callLog.duration > 0 && (callLog.callType == CallLog.Calls.INCOMING_TYPE || callLog.callType == CallLog.Calls.OUTGOING_TYPE)) {
                            append(" · ")
                            append(callLog.duration.secondsToDuration())
                        }
                    },
                    modifier = Modifier.basicMarquee(),
                    style = MaterialTheme.typography.bodyMediumEmphasized
                )
            }
            callLog.location?.let { country ->
                Spacer(Modifier.weight(1f))
                Text(
                    text = country,
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