@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.dialer

import android.content.ClipData
import android.provider.CallLog
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallMissedOutgoing
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.data.actions.CallAction
import com.sosauce.cuteconnect.data.actions.CommonAction
import com.sosauce.cuteconnect.domain.model.CuteCallLog
import com.sosauce.cuteconnect.ui.shared_components.CuteDropdownMenuItem
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import com.sosauce.cuteconnect.ui.shared_components.DropdownItemDelete
import com.sosauce.cuteconnect.utils.betterFormatNumber
import com.sosauce.cuteconnect.utils.getContactNameOrNothing
import com.sosauce.cuteconnect.utils.toReadableDate
import com.sosauce.cuteconnect.utils.toReadableDuration
import com.sosauce.cuteconnect.utils.toReadableTime
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.DurationUnit


@Composable
fun CallLogItem(
    modifier: Modifier = Modifier,
    callLog: CuteCallLog,
    numberOfAppearance: Int,
    onHandleCommonAction: (CommonAction) -> Unit,
    onCallAction: (CallAction) -> Unit
) {

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val phoneUtil = remember(context) { PhoneNumberUtil.getInstance(context) }
    val geocodingUtil = remember(context) { PhoneNumberOfflineGeocoder.getInstance(context) }
    val numberCountry = remember {
        try {
            val numberProto = phoneUtil.parse(callLog.number, Locale.getDefault().country)
            geocodingUtil.getDescriptionForNumber(numberProto, Locale.getDefault())
        } catch (e: NumberParseException) {
           null
        }
    }?.ifEmpty { null } // In this case empty and null both mean no country
    val numberOrName = remember {
        callLog.number.getContactNameOrNothing(context).betterFormatNumber()
    }
    var showMoreOptions by remember { mutableStateOf(false) }

    val iconColor = when(callLog.callType) {
        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.MISSED_TYPE -> Color.Red.copy(0.85f)
        CallLog.Calls.OUTGOING_TYPE -> Color.Green.copy(0.85f)
        CallLog.Calls.REJECTED_TYPE -> Color.Cyan.copy(0.85f)
        else -> LocalContentColor.current
    }

    val icon = when(callLog.callType) {
        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Rounded.CallMissed
        CallLog.Calls.OUTGOING_TYPE -> Icons.Rounded.NorthEast
        CallLog.Calls.REJECTED_TYPE -> Icons.Rounded.SouthWest
        else -> Icons.AutoMirrored.Rounded.CallMissedOutgoing
    }

    val actions = listOf(
        CallLogAction(
            onClick = { onCallAction(CallAction.LaunchCall(callLog.number)) },
            icon = R.drawable.phone,
            text = R.string.call
        ),
        CallLogAction(
            onClick = {  },
            icon = R.drawable.message_rounded,
            text = R.string.send_msg
        ),
        CallLogAction(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(callLog.number, callLog.number)
                        )
                    )
                }
            },
            icon = R.drawable.copy,
            text = R.string.copy_number
        )
    )


    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
//            .combinedClickable(
//                onClick = { onNavigate(Screen.ContactDetails(contact.id)) }
//            )
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            DefaultContactIcon(
                firstLetter = numberOrName.firstOrNull() ?: '?',
                modifier = Modifier
                    .padding(start = 10.dp)
            )
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                CuteText(
                    text = if (numberOfAppearance <= 1) numberOrName else "$numberOrName ($numberOfAppearance)",
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    CuteText(
                        text = buildString {
                            append(callLog.date.toReadableDuration(DurationUnit.MILLISECONDS))
                            append(" · ")
                            append("${callLog.duration.toReadableDuration()}s")
                            numberCountry?.let {
                                append(" ($it)")
                            }
                        },
                        modifier = Modifier.basicMarquee(),
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Row {
            IconButton(
                onClick = { showMoreOptions = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = showMoreOptions,
                onDismissRequest = { showMoreOptions = false },
                shape = RoundedCornerShape(24.dp)
            ) {
                actions.forEach { action ->
                    CuteDropdownMenuItem(
                        onClick = action.onClick,
                        text = {
                            CuteText(
                                text = stringResource(action.text)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(action.icon),
                                contentDescription = null
                            )
                        }
                    )
                }
                DropdownItemDelete(
                    onDelete = {
                        onHandleCommonAction(
                            CommonAction.DeleteFromContentUri(
                                CallLog.Calls.CONTENT_URI,
                                callLog.id
                            )
                        )
                    },
                    dialogTitle = { CuteText(stringResource(R.string.delete_call_log)) },
                    dialogText = { CuteText(stringResource(R.string.delete_call_log_u_sure)) }
                )

            }
        }
    }
}

private data class CallLogAction(
    val onClick: () -> Unit,
    val icon: Int,
    val text: Int
)