@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.contacts.components

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.presentation.shared_components.items.CuteListItem
import com.sosauce.cinnamon.presentation.shared_components.text.HeaderText
import com.sosauce.cinnamon.utils.formateEventDate

@Composable
fun ContactInfos(
    contact: CuteContact
) {

    val resources = LocalResources.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column {

        if (contact.hasInfos) {
            HeaderText(stringResource(R.string.contact_info))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer,)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    contact.details.phoneNumbers.forEachIndexed { index, number ->

                        CuteListItem(
                            onClick = null,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.phone),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .alpha(if (index == 0) 1f else 0f)
                                )
                            }
                        ) {
                            Text(number.number)
                            Text(
                                text = buildString {
                                    append(ContactsContract.CommonDataKinds.Phone.getTypeLabel(resources, number.type, "Custom"))
                                    if (number.isDefault) {
                                        append(" · ")
                                        append(stringResource(R.string.string_default))
                                    }
                                },
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }


                    contact.details.emails.forEachIndexed { index, email ->
                        CuteListItem(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                                    .apply {
                                        putExtra(Intent.EXTRA_EMAIL, email.email)
                                    }

                                context.startActivity(intent)
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.email),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .alpha(if (index == 0) 1f else 0f)
                                )
                            }
                        ) {
                            Text(email.email)
                            Text(
                                text = buildString {
                                    append(ContactsContract.CommonDataKinds.Email.getTypeLabel(resources, email.type, "Custom"))
                                    if (email.isDefault) {
                                        append(" · ")
                                        append(stringResource(R.string.string_default))
                                    }
                                },
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    contact.details.addresses.forEachIndexed { index, address ->
                        CuteListItem(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=${address.address}".toUri())
                                    .apply {
                                        setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")
                                    }
                                context.startActivity(intent)
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.address),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .alpha(if (index == 0) 1f else 0f)
                                )
                            }
                        ) {
                            Text(address.address)
                            Text(
                                text = buildString {
                                    append(ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(resources, address.type, "Custom"))
                                    if (address.isDefault) {
                                        append(" · ")
                                        append(stringResource(R.string.string_default))
                                    }
                                },
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                }
            }
        } else {
            CuteListItem(
                onClick = { /*navigate to edit*/},
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.phone),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            ) { Text(stringResource(R.string.add_phone)) }
            CuteListItem(
                onClick = { /*navigate to edit*/},
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.email),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            ) { Text(stringResource(R.string.add_email)) }
        }


        if (contact.hasAbout) {
            HeaderText(stringResource(R.string.about) + " ${contact.displayName}")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer,)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    contact.details.websites.forEachIndexed { index, website ->
                        CuteListItem(
                            onClick = { uriHandler.openUri(website.website) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.website),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .alpha(if (index == 0) 1f else 0f)
                                )
                            }
                        ) { Text(website.website) }
                    }

                    contact.details.events.forEachIndexed { index, event ->
                        CuteListItem(
                            onClick = null,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.event),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .alpha(if (index == 0) 1f else 0f)
                                )
                            }
                        ) {
                            Text(event.date.formateEventDate())
                            Text(
                                text = ContactsContract.CommonDataKinds.Event.getTypeLabel(resources, event.type, "Custom").toString(),
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    if (contact.details.note?.isNotEmpty() == true) {
                        CuteListItem(
                            onClick = null,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.note),
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        ) { Text(contact.details.note) }
                    }
                }
            }
        }
    }
}