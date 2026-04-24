@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.contacts.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.contacts.ContactDetailsAction
import com.sosauce.cinnamon.presentation.screens.contacts.components.dialogs.NumberPickerDialog
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.utils.getThreadIdOrCreate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun ContactActionsRow(
    contact: CuteContact,
    onNavigate: (Screen) -> Unit,
    onHandleCallAction: (CallAction) -> Unit,
    onHandleContactDetailsAction: (ContactDetailsAction) -> Unit
) {

    val context = LocalContext.current
    val interactionSources = List(4) { remember { MutableInteractionSource() } }
    var showNumberPicker by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf(NumberPickerAction.MESSAGE) }
    val actions = buildList {
        if (contact.details.phoneNumbers.isNotEmpty()) {
            add(
                ContactActionsItem(
                    icon = R.drawable.call,
                    onClick = {
                        if (contact.details.phoneNumbers.size > 1) {
                            action = NumberPickerAction.CALL
                            showNumberPicker = true
                        } else {
                            val number = contact.details.phoneNumbers.first().number
                            onHandleCallAction(CallAction.LaunchCall(number))
                        }
                    }
                )
            )
            add(
                ContactActionsItem(
                    icon = R.drawable.messages_filled,
                    onClick = {
                        if (contact.details.phoneNumbers.size > 1) {
                            action = NumberPickerAction.MESSAGE
                            showNumberPicker = true
                        } else {
                            val threadId = contact.details.phoneNumbers.first().number.getThreadIdOrCreate(context)
                            onNavigate(Screen.Conversation(threadId))
                        }
                    }
                )
            )
        }
        add(
            ContactActionsItem(
                icon = if (contact.isFavorite) R.drawable.favorite_filled else R.drawable.favorite,
                onClick = { onHandleContactDetailsAction(ContactDetailsAction.ToggleFavorite) },
                tint = if (contact.isFavorite) MaterialTheme.colorScheme.error else null
            )
        )
        add(
            ContactActionsItem(
                icon = R.drawable.share,
                onClick = { onHandleContactDetailsAction(ContactDetailsAction.ShareContact) }
            )
        )

    }

    if (showNumberPicker) {
        NumberPickerDialog(
            onDismissRequest = { showNumberPicker = false },
            onPickNumber = { number ->
                when(action) {
                    NumberPickerAction.MESSAGE -> {
                        val threadId = number.getThreadIdOrCreate(context)
                        onNavigate(Screen.Conversation(threadId))
                    }
                    NumberPickerAction.CALL -> onHandleCallAction(CallAction.LaunchCall(number))
                }
            },
            phoneNumbers = contact.details.phoneNumbers.fastMap { it.number }
        )
    }


    ButtonGroup(
        overflowIndicator = {}
    ) {
        actions.forEachIndexed { index, item ->
            customItem(
                {
                    FilledTonalIconButton(
                        onClick = item.onClick,
                        interactionSource = interactionSources[index],
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .weight(1f)
                            .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                            .animateWidth(interactionSources[index])
                    ) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = null
                        )
                    }
                },
                {}
            )
        }
    }
}

private data class ContactActionsItem(
    val id: String = Uuid.random().toString(),
    val icon: Int,
    val onClick: () -> Unit,
    val tint: Color? = null
)

private enum class NumberPickerAction {
    MESSAGE,
    CALL
}