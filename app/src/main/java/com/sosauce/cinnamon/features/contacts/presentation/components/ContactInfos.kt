@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.dialogs.BlockUnblockDialog
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.items.CuteListItemDefaults
import com.sosauce.cinnamon.core.ui.components.text.HeaderText
import com.sosauce.cinnamon.core.utils.formateEventDate
import com.sosauce.cinnamon.core.utils.safeStartActivity
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsAction
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsState
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.components.NoXFound

@Composable
fun ContactInfos(
    state: ContactDetailsState,
    onHandleContactDetailsAction: (ContactDetailsAction) -> Unit,
    onNavigate: (Screen) -> Unit
) {

    val resources = LocalResources.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val hasInfo =
        state.contact.phoneNumbers.isNotEmpty() || state.details.emails.isNotEmpty() || state.details.addresses.isNotEmpty()
    val hasAbout =
        state.details.websites.isNotEmpty() || state.details.note?.isNotEmpty() == true || state.details.events.isNotEmpty()
    val emailOffset = state.contact.phoneNumbers.count()
    val addressOffset = emailOffset + state.details.emails.count()
    val infoItemCount = addressOffset + state.details.addresses.count()
    val eventOffset = state.details.websites.count()
    val noteOffset = eventOffset + state.details.events.count()
    val ringtoneOffset = noteOffset + if (state.details.note?.isNotEmpty() == true) 1 else 0
    val aboutItemCount = ringtoneOffset + 1

    val ringtonePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

            val ringtoneUri = result.data?.let {
                IntentCompat.getParcelableExtra(
                    it,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java
                )
            }
            onHandleContactDetailsAction(ContactDetailsAction.SetRingtone(ringtoneUri))
        }

    var valueToUnblock by remember { mutableStateOf<String?>(null) }

    valueToUnblock?.let { value ->
        BlockUnblockDialog(
            displayName = value,
            isBlocked = true,
            onDismissRequest = { valueToUnblock = null },
            onConfirm = {
                onHandleContactDetailsAction(ContactDetailsAction.Unblock(value))
                valueToUnblock = null
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (hasInfo) {
            Column {
                HeaderText(stringResource(R.string.contact_info))
                state.contact.phoneNumbers.forEachIndexed { index, number ->
                    CuteListItem(
                        onClick = {
                            onHandleContactDetailsAction(
                                ContactDetailsAction.CallNumber(
                                    number.number
                                )
                            )
                        },
                        shape = CuteListItemDefaults.getItemShape(index, infoItemCount),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                        leadingContent = {
                            InfoLeadingIcon(icon = R.drawable.phone, visible = index == 0)
                        },
                        trailingContent = {
                            AnimatedVisibility(
                                visible = number.isBlocked,
                                enter = scaleIn(bouncySpec()),
                                exit = scaleOut(bouncySpec())
                            ) {
                                IconButton(
                                    onClick = { valueToUnblock = number.number },
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier.padding(end = 5.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.block),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            text = number.number,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        InfoTypeLabel(
                            typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                resources,
                                number.type,
                                "Custom"
                            ),
                            isDefault = number.isDefault
                        )
                    }
                }
                state.details.emails.forEachIndexed { index, email ->
                    CuteListItem(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                                .apply {
                                    putExtra(Intent.EXTRA_EMAIL, email.email)
                                }
                            context.safeStartActivity(
                                intent = intent,
                                errorMessage = resources.getString(R.string.no_email_app_found)
                            )

                        },
                        shape = CuteListItemDefaults.getItemShape(
                            emailOffset + index,
                            infoItemCount
                        ),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                        leadingContent = {
                            InfoLeadingIcon(icon = R.drawable.email, visible = index == 0)
                        },
                        trailingContent = {
                            AnimatedVisibility(
                                visible = email.isBlocked,
                                enter = scaleIn(bouncySpec()),
                                exit = scaleOut(bouncySpec())
                            ) {
                                IconButton(
                                    onClick = { valueToUnblock = email.email },
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier.padding(end = 5.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.block),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            text = email.email,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                        )
                        InfoTypeLabel(
                            typeLabel = ContactsContract.CommonDataKinds.Email.getTypeLabel(
                                resources,
                                email.type,
                                "Custom"
                            ),
                            isDefault = email.isDefault
                        )
                    }
                }
                state.details.addresses.forEachIndexed { index, address ->
                    CuteListItem(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "geo:0,0?q=${address.address}".toUri()
                            )
                                .apply {
                                    setClassName(
                                        "com.google.android.apps.maps",
                                        "com.google.android.maps.MapsActivity"
                                    )
                                }
                            context.safeStartActivity(
                                intent = intent,
                                errorMessage = resources.getString(R.string.google_maps_not_found)
                            )
                        },
                        shape = CuteListItemDefaults.getItemShape(
                            addressOffset + index,
                            infoItemCount
                        ),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                        leadingContent = {
                            InfoLeadingIcon(icon = R.drawable.address, visible = index == 0)
                        }
                    ) {
                        Text(address.address)
                        InfoTypeLabel(
                            typeLabel = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(
                                resources,
                                address.type,
                                "Custom"
                            ),
                            isDefault = address.isDefault
                        )
                    }
                }
            }
        }
        if (!hasInfo && !hasAbout) {
            NoXFound(
                headlineText = R.string.no_contact_info,
                bodyText = R.string.no_contact_info_desc,
                icon = R.drawable.info
            )
        }
        Column {
            HeaderText(stringResource(R.string.about) + " ${state.contact.displayName}")
            state.details.websites.forEachIndexed { index, website ->
                CuteListItem(
                    onClick = {
                        try {
                            val formattedWebsite = if (!website.startsWith("https://")) {
                                "https://$website"
                            } else website
                            uriHandler.openUri(formattedWebsite)
                        } catch (_: IllegalArgumentException) {
                            Toast.makeText(context, "Couldn't open website", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    shape = CuteListItemDefaults.getItemShape(index, aboutItemCount),
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                    leadingContent = {
                        InfoLeadingIcon(icon = R.drawable.website, visible = index == 0)
                    }
                ) { Text(website) }
            }
            state.details.events.forEachIndexed { index, event ->
                CuteListItem(
                    onClick = null,
                    shape = CuteListItemDefaults.getItemShape(
                        eventOffset + index,
                        aboutItemCount
                    ),
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                    leadingContent = {
                        InfoLeadingIcon(icon = R.drawable.event, visible = index == 0)
                    }
                ) {
                    Text(event.date.formateEventDate())
                    Text(
                        text = ContactsContract.CommonDataKinds.Event.getTypeLabel(
                            resources,
                            event.type,
                            "Custom"
                        ).toString(),
                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            state.details.note?.let { note ->
                CuteListItem(
                    onClick = null,
                    shape = CuteListItemDefaults.getItemShape(noteOffset, aboutItemCount),
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.note),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                ) { Text(note) }
            }
            CuteListItem(
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, resources.getString(R.string.ringtone))
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                            Settings.System.DEFAULT_RINGTONE_URI
                        )
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            state.details.ringtone ?: Settings.System.DEFAULT_RINGTONE_URI
                        )
                    }
                    try {
                        ringtonePicker.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.set_ringtone_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                shape = CuteListItemDefaults.getItemShape(ringtoneOffset, aboutItemCount),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                leadingContent = {
                    InfoLeadingIcon(icon = R.drawable.ringtone, visible = true)
                }
            ) {
                Text(
                    text = when {
                        state.details.ringtone == null -> stringResource(R.string.default_ringtone)
                        else -> state.ringtoneTitle ?: stringResource(R.string.unknown_ringtone)
                    },
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = stringResource(R.string.ringtone),
                    style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoLeadingIcon(
    @DrawableRes icon: Int,
    visible: Boolean
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier
            .padding(start = 10.dp)
            .alpha(if (visible) 1f else 0f)
    )
}

@Composable
private fun InfoTypeLabel(
    typeLabel: CharSequence,
    isDefault: Boolean
) {
    Text(
        text = buildString {
            append(typeLabel)
            if (isDefault) {
                append(" · ")
                append(stringResource(R.string.string_default))
            }
        },
        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
