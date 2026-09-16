@file:OptIn(ExperimentalGridApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.editor

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.ImagePickerCard
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsActions
import com.sosauce.cinnamon.features.contacts.domain.ContactAddress
import com.sosauce.cinnamon.features.contacts.domain.ContactEmail
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.cinnamon.core.utils.copyMutate
import com.sosauce.nekobites.animations.AnimatedFab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SharedTransitionScope.EditContactScreen(
    state: EditContactState,
    onNavigateUp: () -> Unit,
    onHandleContactSettingsAction: (ContactSettingsActions) -> Unit,
    onHandeEditContactAction: (EditContactAction) -> Unit
) {


    var contact by retain { mutableStateOf(state.contact) }
    var details by retain { mutableStateOf(state.details) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch(Dispatchers.IO) {

                File(state.settings.poster).delete()

                val file = File(
                    context.filesDir,
                    "poster_${state.contact.id}_${System.currentTimeMillis()}.jpg"
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
                        onHandeEditContactAction(EditContactAction.SaveEditedContact(contact, details))
                        //TODO: SEND EVENT WHEN SAVING IS DONE ONLY THEN NAVIGATE BACK
                        //onNavigateUp()
                    },
                    icon = R.drawable.check,
                    enabled = contact != state.contact || details != state.details
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                EditContactPfp(
                    modifier = Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_PFP),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current
                    ),
                    pfp = details.photo,
                    onPfpSelected = { newPhoto ->
                        details = details.copy(photoString = newPhoto.toString())
                    },
                    onRemoveImage = {
                        details = details.copy(photoString = null)
                    }
                )

                if (!state.isCreateInsteadOfEdit) {
                    ImagePickerCard(
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
                        },
                        image = state.settings.poster.ifEmpty { null }?.toUri(),
                        modifier = Modifier
                            .height(250.dp)
                            .width(150.dp)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_POSTER),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                    )
                }
            }

            Spacer(Modifier.height(25.dp))

            ContactEditTextField(
                value = details.firstName ?: "",
                label = R.string.first_name,
                leadingIcon = R.drawable.contact,
                onValueChange = {
                    details = details.copy(firstName = it)
                },
                onClickRemove = null
            )
            ContactEditTextField(
                value = details.lastName ?: "",
                label = R.string.last_name,
                leadingIcon = R.drawable.contact,
                onValueChange = {
                    details = details.copy(lastName = it)
                },
                onClickRemove = null
            )


            if (!details.company.isNullOrEmpty()) {
                ContactEditTextField(
                    value = details.company ?: "",
                    label = R.string.company,
                    leadingIcon = R.drawable.business,
                    onValueChange = {
                        details = details.copy(company = it)
                    },
                    onClickRemove = {
                        details = details.copy(company = "")
                    }
                )
            }

            Spacer(Modifier.height(25.dp))

            ContactDataSection(
                items = contact.phoneNumbers,
                keyboardType = KeyboardType.Phone,
                labelRes = R.string.phone,
                iconRes = R.drawable.phone,
                addLabelRes = R.string.add_phone,
                valueProvider = { it.number },
                onValueChange = { index, value ->
                    contact = contact.copy(
                        phoneNumbers = contact.phoneNumbers.copyMutate {
                            this[index] = this[index].copy(number = value)
                        }
                    )
                },

                onRemove = { index ->
                    contact = contact.copy(
                        phoneNumbers = contact.phoneNumbers.copyMutate {
                            removeAt(index)
                        }
                    )
                },

                onAdd = {
                    contact = contact.copy(
                        phoneNumbers = contact.phoneNumbers + ContactPhone(
                            "",
                            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,
                            true
                        )
                    )
                }
            )


            ContactDataSection(
                items = details.emails,
                keyboardType = KeyboardType.Email,
                labelRes = R.string.email,
                iconRes = R.drawable.email,
                addLabelRes = R.string.add_email,
                valueProvider = { it.email },

                onValueChange = { index, value ->
                    details = details.copy(
                        emails = details.emails.copyMutate {
                            this[index] = this[index].copy(email = value)
                        }
                    )
                },

                onRemove = { index ->
                    details = details.copy(
                        emails = details.emails.copyMutate {
                            removeAt(index)
                        }
                    )
                },

                onAdd = {
                    details = details.copy(
                        emails = details.emails + ContactEmail(
                            "",
                            ContactsContract.CommonDataKinds.Email.TYPE_OTHER,
                            true
                        )
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            ContactDataSection(
                items = details.addresses,
                keyboardType = KeyboardType.PostalAddress,
                labelRes = R.string.address,
                iconRes = R.drawable.address,
                addLabelRes = R.string.add_address,
                valueProvider = { it.address },

                onValueChange = { index, value ->
                    details = details.copy(
                        addresses = details.addresses.copyMutate {
                            this[index] = this[index].copy(address = value)
                        }
                    )
                },
                

                onRemove = { index ->
                    details = details.copy(
                        addresses = details.addresses.copyMutate {
                            removeAt(index)
                        }
                    )
                },

                onAdd = {
                    details = details.copy(
                        addresses = details.addresses + ContactAddress(
                            "",
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER,
                            true
                        )
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            ContactDataSection(
                items = details.websites,
                labelRes = R.string.website,
                iconRes = R.drawable.website,
                addLabelRes = R.string.add_website,
                valueProvider = { it },
                onValueChange = { index, value ->
                    details = details.copy(
                        websites = details.websites.copyMutate {
                            this[index] = value
                        }
                    )
                },

                onRemove = { index ->
                    details = details.copy(
                        websites = details.websites.copyMutate {
                            removeAt(index)
                        }
                    )
                },

                onAdd = {
                    details = details.copy(
                        websites = details.websites + ""
                    )
                }
            )

            Spacer(Modifier.height(10.dp))


            details.note?.let { note ->
                ContactEditTextField(
                    value = note,
                    label = R.string.notes,
                    leadingIcon = R.drawable.note,
                    onValueChange = {
                        details = details.copy(note = it)
                    },
                    onClickRemove = {
                        details = details.copy(note = null)
                    }
                )

            }


            Spacer(Modifier.weight(1f))

            // ADD BUTTONS
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AddDataButton(
                    isVisible = contact.phoneNumbers.isEmpty(),
                    icon = R.drawable.phone,
                    text = R.string.add_phone,
                    onClick = {
                        contact = contact.copy(
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
                    isVisible = details.emails.isEmpty(),
                    icon = R.drawable.email,
                    text = R.string.add_email,
                    onClick = {
                        details = details.copy(
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
                    isVisible = details.addresses.isEmpty(),
                    icon = R.drawable.address,
                    text = R.string.add_address,
                    onClick = {
                        details = details.copy(
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
                    isVisible = details.websites.isEmpty(),
                    icon = R.drawable.website,
                    text = R.string.add_website,
                    onClick = {
                        details = details.copy(
                            websites = listOf("")
                        )
                    }
                )
                AddDataButton(
                    isVisible = details.note == null,
                    icon = R.drawable.note,
                    text = R.string.add_note,
                    onClick = {
                        details = details.copy(
                            note = ""
                        )
                    }
                )
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

    LaunchedEffect(state.text) { onValueChange(state.text.toString()) }


    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        state = state,
        shape = RoundedCornerShape(12.dp),
        label = { Text(stringResource(label)) },
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
        trailingIcon = {
            if (onClickRemove != null) {
                IconButton(
                    onClick = onClickRemove,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.remove_all_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun EditContactPfp(
    modifier: Modifier = Modifier,
    pfp: Uri?,
    onPfpSelected: (Uri) -> Unit,
    onRemoveImage: () -> Unit
) {

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onPfpSelected(it) }
        }
    Box(
        modifier
            .size(170.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, end = 12.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(170.dp / 2)
            )
            AsyncImage(
                model = pfp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        AnimatedVisibility(
            visible = pfp != null && pfp != Uri.EMPTY,
            enter = scaleIn(bouncySpec()),
            exit = scaleOut(bouncySpec()),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            FilledIconButton(
                onClick = onRemoveImage
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null
                )
            }
        }
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
        Column {
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