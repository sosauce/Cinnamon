@file:OptIn(ExperimentalGridApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.cinnamon.core.utils.copyMutate
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsActions
import com.sosauce.cinnamon.features.contacts.domain.ContactAddress
import com.sosauce.cinnamon.features.contacts.domain.ContactEmail
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SharedTransitionScope.EditContactScreen(
    state: EditContactState,
    onNavigateUp: () -> Unit,
    onHandleContactSettingsAction: (ContactSettingsActions) -> Unit,
    onHandeEditContactAction: (EditContactAction) -> Unit
) {



    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

            if (uri == null) return@rememberLauncherForActivityResult

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

    LoadingBox(
        isLoading = state.isLoading
    ) {

        var rawContact by retain { mutableStateOf(state.rawContact) }
        val pfpPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { rawContact = rawContact.copy(photoString = it.toString()) }
        }

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
                        onNavigateUp = onNavigateUp
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
                            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
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


                    if (!rawContact.company.isNullOrEmpty()) {
                        ContactEditTextField(
                            value = rawContact.company ?: "",
                            label = R.string.company,
                            leadingIcon = R.drawable.business,
                            onValueChange = {
                                rawContact = rawContact.copy(company = it)
                            },
                            onClickRemove = {
                                rawContact = rawContact.copy(company = "")
                            }
                        )
                    }
                }

                Spacer(Modifier.height(25.dp))

                ContactDataSection(
                    items = rawContact.phoneNumbers,
                    keyboardType = KeyboardType.Phone,
                    labelRes = R.string.phone,
                    iconRes = R.drawable.phone,
                    addLabelRes = R.string.add_phone,
                    valueProvider = { it.number },
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
                                true
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
                                true
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
                                true
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
    onClickRemove: (() -> Unit)?
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
        TextField(
            state = state,
            label = { Text(text = stringResource(label), maxLines = 1, modifier = Modifier.basicMarquee()) },
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
                                .background(colors.primaryContainer, MaterialShapes.Cookie9Sided.toShape())
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
    valueProvider: (T) -> String
) {
    if (items.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            FieldGroup {
                items.fastForEachIndexed { index, item ->
                    ContactEditTextField(
                        value = valueProvider(item),
                        keyboardType = keyboardType,
                        label = labelRes,
                        leadingIcon = iconRes,
                        onValueChange = { newValue -> onValueChange(index, newValue) },
                        onClickRemove = { onRemove(index) }
                    )
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
