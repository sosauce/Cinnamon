@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.items.CuteListItemDefaults
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.presentation.ContactListItem
import com.sosauce.cinnamon.features.contacts.presentation.ContactsViewModel
import com.sosauce.cinnamon.features.contacts.presentation.components.FavoriteContact
import com.sosauce.cinnamon.features.contacts.presentation.components.dialogs.NumberPickerDialog
import com.sosauce.cinnamon.features.contacts.presentation.contactsList
import com.sosauce.cinnamon.settings.SpeedDialViewModel
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import org.koin.androidx.compose.koinViewModel

@Composable
fun SpeedDialDialog(
    onDismissRequest: () -> Unit
) {
    val viewModel = koinViewModel<SpeedDialViewModel>()
    val speedDials by viewModel.speedDials.collectAsStateWithLifecycle()
    var pickingDigit by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.dialpad),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.speed_dial)) },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.close))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                (1..9).forEach { digit ->
                    val speedDial = speedDials[digit]
                    CuteListItem(
                        onClick = { pickingDigit = digit },
                        shape = CuteListItemDefaults.getItemShape(digit - 1, 9),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        leadingContent = {
                            Text(
                                text = "$digit.",
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        },
                        trailingContent = speedDial?.let {
                            {
                                IconButton(
                                    onClick = { viewModel.unassign(digit) },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = stringResource(R.string.unassign_speed_dial)
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            text = speedDial?.name
                                ?: speedDial?.number?.beautifyNumber()
                                ?: stringResource(R.string.not_assigned),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = if (speedDial == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        if (speedDial?.name != null) {
                            Text(
                                text = speedDial.number.beautifyNumber(),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }
        }
    )

    pickingDigit?.let { digit ->
        SpeedDialContactPicker(
            onDismissRequest = { pickingDigit = null },
            onPickNumber = { number ->
                viewModel.assign(digit, number)
                pickingDigit = null
            }
        )
    }
}

@Composable
private fun SpeedDialContactPicker(
    onDismissRequest: () -> Unit,
    onPickNumber: (String) -> Unit
) {
    val contactsViewModel = koinViewModel<ContactsViewModel>()
    val state by contactsViewModel.state.collectAsStateWithLifecycle()
    val contacts = contactsViewModel.contacts.collectAsLazyPagingItems()
    var numberPickerContact by remember { mutableStateOf<CuteContact?>(null) }

    val onContactClick: (CuteContact) -> Unit = { contact ->
        when (contact.phoneNumbers.size) {
            0 -> Unit
            1 -> onPickNumber(contact.phoneNumbers.first().number)
            else -> numberPickerContact = contact
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                bottomBar = { CuteNavigationButton(onNavigateUp = onDismissRequest) }
            ) { paddingValues ->
                SharedTransitionLayout {
                    LoadingBox(
                        isLoading = state.isLoading
                    ) {
                        LazyColumn(
                            contentPadding = paddingValues,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            contactsList(
                                favorites = state.favorites,
                                contacts = contacts,
                                emptyState = {
                                    NoXFound(
                                        headlineText = R.string.no_contacts_found,
                                        bodyText = R.string.no_contacts_found_desc,
                                        icon = R.drawable.contacts
                                    )
                                },
                                favoriteContactItem = { contact ->
                                    FavoriteContact(
                                        modifier = Modifier.animateItem(),
                                        contact = contact,
                                        isSelected = false,
                                        onClick = { onContactClick(contact) },
                                        onLongClick = {},
                                        showNumber = true
                                    )
                                }
                            ) { contact ->
                                ContactListItem(
                                    modifier = Modifier.animateItem(),
                                    contact = contact,
                                    isSelected = false,
                                    onClick = { onContactClick(contact) },
                                    showNumber = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    numberPickerContact?.let { contact ->
        NumberPickerDialog(
            onDismissRequest = { numberPickerContact = null },
            onPickNumber = { number ->
                numberPickerContact = null
                onPickNumber(number)
            },
            phoneNumbers = contact.phoneNumbers.fastMap { it.number }
        )
    }
}
