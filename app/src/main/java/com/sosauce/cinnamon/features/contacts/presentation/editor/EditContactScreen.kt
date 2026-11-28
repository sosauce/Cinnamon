@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)

package com.sosauce.cinnamon.features.contacts.presentation.editor

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.features.contacts.presentation.crop.CropMode
import com.sosauce.cinnamon.features.contacts.presentation.crop.ImageCropperDialog
import com.sosauce.cinnamon.core.utils.copyMutate
import com.sosauce.cinnamon.core.utils.formateEventDate
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsActions
import com.sosauce.cinnamon.features.contacts.domain.ContactAddress
import com.sosauce.cinnamon.features.contacts.domain.ContactEmail
import com.sosauce.cinnamon.features.contacts.domain.ContactEvent
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SharedTransitionScope.EditContactScreen(
    state: EditContactState,
    onNavigateUp: () -> Unit,
    onHandleContactSettingsAction: (ContactSettingsActions) -> Unit,
    onHandeEditContactAction: (EditContactAction) -> Unit
) {

    var rawContact by retain(state.rawContact) { mutableStateOf(state.rawContact) }
    var showUnsavedChangesWarning by remember { mutableStateOf(false) }

    val hasUnsavedChanges = state.rawContact != rawContact

    BackHandler(hasUnsavedChanges) {
        showUnsavedChangesWarning = true
    }

    if (showUnsavedChangesWarning) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesWarning = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_u_sure)) },
            dismissButton = {
                TextButton(
                    onClick = { showUnsavedChangesWarning = false },
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedChangesWarning = false
                        onNavigateUp()
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = stringResource(R.string.discard),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var pendingCrop by remember { mutableStateOf<Pair<Uri, CropMode>?>(null) }

    fun savePoster(uri: Uri) {
        scope.launch(Dispatchers.IO) {

            File(state.settings.poster).delete()

            val file = File(
                context.filesDir,
                "poster_${state.rawContact.contactId}_${System.currentTimeMillis()}.jpg"
            )

            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }

            onHandleContactSettingsAction(
                ContactSettingsActions.UpsertContactSettings(
                    state.settings.copy(
                        poster = file.path
                    )
                )
            )
        }
    }

    pendingCrop?.let { (uri, mode) ->
        ImageCropperDialog(
            uri = uri,
            mode = mode,
            onDismissRequest = { pendingCrop = null },
            onCropped = { cropped ->
                pendingCrop = null
                when (mode) {
                    CropMode.ProfilePicture -> rawContact = rawContact.copy(photoString = cropped.toString())
                    CropMode.Poster -> savePoster(cropped)
                }
            }
        )
    }

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { pendingCrop = it to CropMode.Poster }
        }
    val pfpPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { pendingCrop = it to CropMode.ProfilePicture }
        }
    // index of the event being edited, -1 when adding a new one
    var eventPickerIndex by remember { mutableStateOf<Int?>(null) }

    eventPickerIndex?.let { index ->

        val relatedEvent = rawContact.events.getOrNull(index)
        val initialSelectedDate = relatedEvent?.date?.let { date ->
            runCatching {
                LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDate
        )

        DatePickerDialog(
            onDismissRequest = { eventPickerIndex = null },
            dismissButton = {
                TextButton(
                    onClick = { eventPickerIndex = null },
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis =
                            datePickerState.selectedDateMillis ?: return@TextButton
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)
                            .toLocalDate().toString()

                        rawContact = rawContact.copy(
                            events = if (relatedEvent != null) {
                                rawContact.events.copyMutate {
                                    this[index] = this[index].copy(date = date)
                                }
                            } else {
                                rawContact.events + ContactEvent(
                                    date,
                                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                )
                            }
                        )
                        eventPickerIndex = null
                    },
                    shapes = ButtonDefaults.shapes(),
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text(stringResource(R.string.set)) }
            }
        ) { DatePicker(datePickerState) }
    }

    LoadingBox(
        isLoading = state.isLoading
    ) {
        Scaffold(
            bottomBar = {
                Row(
                    modifier = Modifier
                        .imePadding()
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CuteNavigationButtonSurface(
                        modifier = Modifier
                            .padding(horizontal = 15.dp)
                            .navigationBarsPadding(),
                        onNavigateUp = {
                            if (hasUnsavedChanges) {
                                showUnsavedChangesWarning = true
                            } else {
                                onNavigateUp()
                            }
                        }
                    )
                    AnimatedFab(
                        onClick = {
                            onHandeEditContactAction(
                                EditContactAction.SaveEditedContact(rawContact)
                            )
                        },
                        icon = R.drawable.check,
                        enabled = rawContact != state.rawContact
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val availableWidth = maxWidth - 20.dp
                        val profileWidth = if (state.isCreateInsteadOfEdit) {
                            minOf(maxWidth, 180.dp)
                        } else {
                            availableWidth * 0.57f
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                20.dp,
                                Alignment.CenterHorizontally
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ContactImagePickerTile(
                                image = rawContact.photo,
                                label = stringResource(R.string.contact_photo),
                                isPoster = false,
                                tileWidth = profileWidth,
                                onClick = {
                                    pfpPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemoveImage = {
                                    rawContact = rawContact.copy(photoString = null)
                                }
                            )

                            if (!state.isCreateInsteadOfEdit) {
                                ContactImagePickerTile(
                                    image = state.settings.poster.ifEmpty { null }?.toUri(),
                                    label = stringResource(R.string.call_poster),
                                    isPoster = true,
                                    tileWidth = availableWidth * 0.43f,
                                    onClick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onRemoveImage = {
                                        scope.launch(Dispatchers.IO) {
                                            File(context.filesDir, state.settings.poster).delete()
                                            onHandleContactSettingsAction(
                                                ContactSettingsActions.UpsertContactSettings(
                                                    state.settings.copy(poster = "")
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(25.dp))

                FieldGroup {
                    ContactEditTextField(
                        value = rawContact.firstName ?: "",
                        label = R.string.first_name,
                        leadingIcon = R.drawable.contact,
                        onValueChange = {
                            rawContact = rawContact.copy(firstName = it)
                        },
                        onClickRemove = null
                    )
                    ContactEditTextField(
                        value = rawContact.middleName ?: "",
                        label = R.string.middle_name,
                        leadingIcon = R.drawable.contact,
                        onValueChange = {
                            rawContact = rawContact.copy(middleName = it)
                        },
                        onClickRemove = null
                    )
                    ContactEditTextField(
                        value = rawContact.lastName ?: "",
                        label = R.string.last_name,
                        leadingIcon = R.drawable.contact,
                        onValueChange = {
                            rawContact = rawContact.copy(lastName = it)
                        },
                        onClickRemove = null
                    )
                    ContactEditTextField(
                        value = rawContact.company ?: "",
                        label = R.string.company,
                        leadingIcon = R.drawable.company,
                        onValueChange = {
                            rawContact = rawContact.copy(company = it)
                        },
                        onClickRemove = null
                    )
                }

                Spacer(Modifier.height(25.dp))

                ContactDataSection(
                    items = rawContact.phoneNumbers,
                    keyboardType = KeyboardType.Phone,
                    labelRes = R.string.phone,
                    iconRes = R.drawable.phone,
                    addLabelRes = R.string.add_phone,
                    valueProvider = { it.number },
                    rowExtras = { index, phone ->
                        TypeAndDefaultRow(
                            type = phone.type,
                            types = PhoneTypes,
                            typeLabel = {
                                ContactsContract.CommonDataKinds.Phone
                                    .getTypeLabel(resources, it, resources.getString(R.string.custom_label)).toString()
                            },
                            isDefault = phone.isDefault,
                            onTypeChange = { type ->
                                rawContact = rawContact.copy(
                                    phoneNumbers = rawContact.phoneNumbers.copyMutate {
                                        this[index] = this[index].copy(type = type)
                                    }
                                )
                            },
                            onToggleDefault = {
                                rawContact = rawContact.copy(
                                    phoneNumbers = rawContact.phoneNumbers.mapIndexed { i, item ->
                                        item.copy(isDefault = i == index && !item.isDefault)
                                    }
                                )
                            }
                        )
                    },
                    onValueChange = { index, value ->
                        rawContact = rawContact.copy(
                            phoneNumbers = rawContact.phoneNumbers.copyMutate {
                                this[index] = this[index].copy(number = value)
                            }
                        )
                    },

                    onRemove = { index ->
                        rawContact = rawContact.copy(
                            phoneNumbers = rawContact.phoneNumbers.copyMutate {
                                removeAt(index)
                            }
                        )
                    },

                    onAdd = {
                        rawContact = rawContact.copy(
                            phoneNumbers = rawContact.phoneNumbers + ContactPhone(
                                "",
                                ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,
                                false
                            )
                        )
                    }
                )


                ContactDataSection(
                    items = rawContact.emails,
                    keyboardType = KeyboardType.Email,
                    labelRes = R.string.email,
                    iconRes = R.drawable.email,
                    addLabelRes = R.string.add_email,
                    valueProvider = { it.email },
                    rowExtras = { index, email ->
                        TypeAndDefaultRow(
                            type = email.type,
                            types = EmailTypes,
                            typeLabel = {
                                ContactsContract.CommonDataKinds.Email
                                    .getTypeLabel(resources, it, resources.getString(R.string.custom_label)).toString()
                            },
                            isDefault = email.isDefault,
                            onTypeChange = { type ->
                                rawContact = rawContact.copy(
                                    emails = rawContact.emails.copyMutate {
                                        this[index] = this[index].copy(type = type)
                                    }
                                )
                            },
                            onToggleDefault = {
                                rawContact = rawContact.copy(
                                    emails = rawContact.emails.mapIndexed { i, item ->
                                        item.copy(isDefault = i == index && !item.isDefault)
                                    }
                                )
                            }
                        )
                    },

                    onValueChange = { index, value ->
                        rawContact = rawContact.copy(
                            emails = rawContact.emails.copyMutate {
                                this[index] = this[index].copy(email = value)
                            }
                        )
                    },

                    onRemove = { index ->
                        rawContact = rawContact.copy(
                            emails = rawContact.emails.copyMutate {
                                removeAt(index)
                            }
                        )
                    },

                    onAdd = {
                        rawContact = rawContact.copy(
                            emails = rawContact.emails + ContactEmail(
                                "",
                                ContactsContract.CommonDataKinds.Email.TYPE_OTHER,
                                false
                            )
                        )
                    }
                )

                Spacer(Modifier.height(10.dp))

                ContactDataSection(
                    items = rawContact.addresses,
                    keyboardType = KeyboardType.PostalAddress,
                    labelRes = R.string.address,
                    iconRes = R.drawable.address,
                    addLabelRes = R.string.add_address,
                    valueProvider = { it.address },
                    rowExtras = { index, address ->
                        TypeAndDefaultRow(
                            type = address.type,
                            types = AddressTypes,
                            typeLabel = {
                                ContactsContract.CommonDataKinds.StructuredPostal
                                    .getTypeLabel(resources, it, resources.getString(R.string.custom_label)).toString()
                            },
                            isDefault = address.isDefault,
                            onTypeChange = { type ->
                                rawContact = rawContact.copy(
                                    addresses = rawContact.addresses.copyMutate {
                                        this[index] = this[index].copy(type = type)
                                    }
                                )
                            },
                            onToggleDefault = {
                                rawContact = rawContact.copy(
                                    addresses = rawContact.addresses.mapIndexed { i, item ->
                                        item.copy(isDefault = i == index && !item.isDefault)
                                    }
                                )
                            }
                        )
                    },

                    onValueChange = { index, value ->
                        rawContact = rawContact.copy(
                            addresses = rawContact.addresses.copyMutate {
                                this[index] = this[index].copy(address = value)
                            }
                        )
                    },


                    onRemove = { index ->
                        rawContact = rawContact.copy(
                            addresses = rawContact.addresses.copyMutate {
                                removeAt(index)
                            }
                        )
                    },

                    onAdd = {
                        rawContact = rawContact.copy(
                            addresses = rawContact.addresses + ContactAddress(
                                "",
                                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER,
                                false
                            )
                        )
                    }
                )

                Spacer(Modifier.height(10.dp))

                ContactDataSection(
                    items = rawContact.websites,
                    labelRes = R.string.website,
                    iconRes = R.drawable.website,
                    addLabelRes = R.string.add_website,
                    valueProvider = { it },
                    onValueChange = { index, value ->
                        rawContact = rawContact.copy(
                            websites = rawContact.websites.copyMutate {
                                this[index] = value
                            }
                        )
                    },

                    onRemove = { index ->
                        rawContact = rawContact.copy(
                            websites = rawContact.websites.copyMutate {
                                removeAt(index)
                            }
                        )
                    },

                    onAdd = {
                        rawContact = rawContact.copy(
                            websites = rawContact.websites + ""
                        )
                    }
                )

                Spacer(Modifier.height(10.dp))

                if (rawContact.events.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        FieldGroup {
                            rawContact.events.fastForEachIndexed { index, event ->
                                ContactEventRow(
                                    date = event.date.formateEventDate(),
                                    type = event.type,
                                    typeLabel = {
                                        ContactsContract.CommonDataKinds.Event
                                            .getTypeLabel(resources, it, resources.getString(R.string.custom_label)).toString()
                                    },
                                    onTypeChange = { type ->
                                        rawContact = rawContact.copy(
                                            events = rawContact.events.copyMutate {
                                                this[index] = this[index].copy(type = type)
                                            }
                                        )
                                    },
                                    onClick = { eventPickerIndex = index },
                                    onClickRemove = {
                                        rawContact = rawContact.copy(
                                            events = rawContact.events.copyMutate {
                                                removeAt(index)
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = { eventPickerIndex = -1 },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.add_event))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))


                rawContact.note?.let { note ->
                    FieldGroup {
                        ContactEditTextField(
                            value = note,
                            label = R.string.notes,
                            leadingIcon = R.drawable.note,
                            onValueChange = {
                                rawContact = rawContact.copy(note = it)
                            },
                            onClickRemove = {
                                rawContact = rawContact.copy(note = null)
                            }
                        )
                    }

                }


                Spacer(Modifier.weight(1f))

                // ADD BUTTONS
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    AddDataButton(
                        isVisible = rawContact.phoneNumbers.isEmpty(),
                        icon = R.drawable.phone,
                        text = R.string.add_phone,
                        onClick = {
                            rawContact = rawContact.copy(
                                phoneNumbers = listOf(
                                    ContactPhone(
                                        "",
                                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                                        true
                                    )
                                )
                            )
                        }
                    )

                    AddDataButton(
                        isVisible = rawContact.emails.isEmpty(),
                        icon = R.drawable.email,
                        text = R.string.add_email,
                        onClick = {
                            rawContact = rawContact.copy(
                                emails = listOf(
                                    ContactEmail(
                                        "",
                                        ContactsContract.CommonDataKinds.Email.TYPE_OTHER,
                                        true
                                    )
                                )
                            )
                        }
                    )

                    AddDataButton(
                        isVisible = rawContact.addresses.isEmpty(),
                        icon = R.drawable.address,
                        text = R.string.add_address,
                        onClick = {
                            rawContact = rawContact.copy(
                                addresses = listOf(
                                    ContactAddress(
                                        "",
                                        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER,
                                        true
                                    )
                                )
                            )
                        }
                    )
                    AddDataButton(
                        isVisible = rawContact.websites.isEmpty(),
                        icon = R.drawable.website,
                        text = R.string.add_website,
                        onClick = {
                            rawContact = rawContact.copy(
                                websites = listOf("")
                            )
                        }
                    )
                    AddDataButton(
                        isVisible = rawContact.events.isEmpty(),
                        icon = R.drawable.event,
                        text = R.string.add_event,
                        onClick = { eventPickerIndex = -1 }
                    )
                    AddDataButton(
                        isVisible = rawContact.note == null,
                        icon = R.drawable.note,
                        text = R.string.add_note,
                        onClick = {
                            rawContact = rawContact.copy(
                                note = ""
                            )
                        }
                    )
                }
            }
        }
    }

}

@Composable
private fun AddDataButton(
    onClick: () -> Unit,
    isVisible: Boolean,
    @DrawableRes icon: Int = R.drawable.add,
    @StringRes text: Int
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(bouncySpec()),
        exit = scaleOut(bouncySpec())
    ) {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapes()
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null
            )
            Spacer(Modifier.width(5.dp))
            Text(stringResource(text))
        }
    }
}

@Composable
private fun ContactEditTextField(
    modifier: Modifier = Modifier,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    @StringRes label: Int,
    @DrawableRes leadingIcon: Int,
    onValueChange: (String) -> Unit,
    onClickRemove: (() -> Unit)?,
    bottomContent: (@Composable () -> Unit)? = null
) {

    val state = rememberTextFieldState(initialText = value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .distinctUntilChanged()
            .collect(currentOnValueChange)
    }


    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val corner by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 2.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val container by animateColorAsState(
        if (isFocused) colors.surfaceContainerHigh else colors.surfaceContainer
    )

    Surface(
        shape = RoundedCornerShape(corner),
        color = container,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            TextField(
                state = state,
                label = {
                    Text(
                        text = stringResource(label),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(leadingIcon),
                        contentDescription = null
                    )
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType
                ),
                interactionSource = interactionSource,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                trailingIcon = {
                    if (onClickRemove != null) {
                        IconButton(
                            onClick = onClickRemove,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.remove_all_filled),
                                contentDescription = stringResource(R.string.remove_field),
                                tint = colors.error
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            bottomContent?.invoke()
        }
    }
}

@Composable
private fun FieldGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        content = content
    )
}

@Composable
private fun ContactImagePickerTile(
    modifier: Modifier = Modifier,
    image: Uri?,
    label: String,
    isPoster: Boolean,
    tileWidth: Dp,
    onClick: () -> Unit,
    onRemoveImage: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = if (isPoster) RoundedCornerShape(28.dp) else MaterialShapes.Cookie9Sided.toShape()
    val tileModifier = Modifier
        .width(tileWidth)
        .height(if (isPoster) tileWidth * 1.46f else tileWidth)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(tileModifier) {
            Surface(
                onClick = onClick,
                shape = shape,
                color = colors.surfaceContainerHigh,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (image != null && image != Uri.EMPTY) {
                        AsyncImage(
                            model = image,
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(if (isPoster) 52.dp else 66.dp)
                                .background(
                                    colors.primaryContainer,
                                    MaterialShapes.Cookie9Sided.toShape()
                                )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add_photo),
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(if (isPoster) 26.dp else 32.dp)
                            )
                        }
                    }
                }
            }

            FilledIconButton(
                onClick = onClick,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(if (image == null || image == Uri.EMPTY) R.drawable.add else R.drawable.edit_filled),
                    contentDescription = stringResource(R.string.choose_image),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (image != null && image != Uri.EMPTY) {
                FilledIconButton(
                    onClick = onRemoveImage,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.remove_image),
                    )
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = colors.onSurface
        )
    }
}

@Composable
private fun <T> ContactDataSection(
    items: List<T>,
    keyboardType: KeyboardType = KeyboardType.Text,
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    @StringRes addLabelRes: Int,
    onValueChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    valueProvider: (T) -> String,
    rowExtras: (@Composable (Int, T) -> Unit)? = null
) {
    if (items.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            FieldGroup {
                // text field states are positional, rebuild them when a row is added/removed
                // so every field keeps showing its own value
                key(items.size) {
                    items.fastForEachIndexed { index, item ->
                        ContactEditTextField(
                            value = valueProvider(item),
                            keyboardType = keyboardType,
                            label = labelRes,
                            leadingIcon = iconRes,
                            onValueChange = { newValue -> onValueChange(index, newValue) },
                            onClickRemove = { onRemove(index) },
                            bottomContent = rowExtras?.let { extras ->
                                @Composable { extras(index, item) }
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(addLabelRes))
            }
        }
    }
}

private val PhoneTypes = listOf(
    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
    ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
    ContactsContract.CommonDataKinds.Phone.TYPE_MAIN,
    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
)

private val EmailTypes = listOf(
    ContactsContract.CommonDataKinds.Email.TYPE_HOME,
    ContactsContract.CommonDataKinds.Email.TYPE_WORK,
    ContactsContract.CommonDataKinds.Email.TYPE_MOBILE,
    ContactsContract.CommonDataKinds.Email.TYPE_OTHER
)

private val AddressTypes = listOf(
    ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
    ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK,
    ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
)

private val EventTypes = listOf(
    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
    ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,
    ContactsContract.CommonDataKinds.Event.TYPE_OTHER
)

@Composable
private fun TypeAndDefaultRow(
    type: Int,
    types: List<Int>,
    typeLabel: (Int) -> String,
    isDefault: Boolean,
    onTypeChange: (Int) -> Unit,
    onToggleDefault: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val corners by animateDpAsState(if (isDefault) 50.dp else 12.dp)
    val defaultColor by animateColorAsState(
        if (isDefault) colors.primary else colors.surfaceContainerHighest
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
    ) {
        TypeMenuButton(
            type = type,
            types = types,
            typeLabel = typeLabel,
            onTypeChange = onTypeChange
        )

        Button(
            onClick = onToggleDefault,
            shape = RoundedCornerShape(corners),
            colors = ButtonDefaults.buttonColors(
                containerColor = defaultColor,
                contentColor = if (isDefault) colors.onPrimary else colors.onSurface
            )
        ) {
            AnimatedVisibility(
                visible = isDefault,
                modifier = Modifier.padding(end = 5.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null
                )
            }
            Text(stringResource(R.string.string_default))
        }
    }
}

@Composable
private fun TypeMenuButton(
    type: Int,
    types: List<Int>,
    typeLabel: (Int) -> String,
    onTypeChange: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceContainerHighest,
                contentColor = colors.onSurface
            )
        ) {
            Text(
                text = typeLabel(type),
                maxLines = 1
            )
        }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes()
            ) {
                types.fastForEachIndexed { index, option ->
                    SelectableDropdownMenuItem(
                        selected = type == option,
                        onClick = {
                            onTypeChange(option)
                            expanded = false
                        },
                        shapes = MenuDefaults.itemShape(index, types.count()),
                        trailingContent = {
                            if (option == type) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null
                                )
                            }
                        },
                        text = { Text(typeLabel(option)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactEventRow(
    date: String,
    type: Int,
    typeLabel: (Int) -> String,
    onTypeChange: (Int) -> Unit,
    onClick: () -> Unit,
    onClickRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(2.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.event),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant
                )
                Text(
                    text = date,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .basicMarquee()
                )
                IconButton(
                    onClick = onClickRemove,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.remove_all_filled),
                        contentDescription = stringResource(R.string.remove_field),
                        tint = colors.error
                    )
                }
            }
            TypeMenuButton(
                type = type,
                types = EventTypes,
                typeLabel = typeLabel,
                onTypeChange = onTypeChange
            )
        }
    }
}
