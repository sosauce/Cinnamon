@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsAction
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsState
import com.sosauce.cinnamon.features.contacts.presentation.components.dialogs.NumberPickerDialog
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun ContactActionsRow(
    state: ContactDetailsState,
    onNavigate: (Screen) -> Unit,
    onHandleContactDetailsAction: (ContactDetailsAction) -> Unit,
    onPlayFavoriteAnimation: () -> Unit
) {

    val context = LocalContext.current
    val interactionSources = List(4) { remember { MutableInteractionSource() } }
    var showNumberPicker by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf(NumberPickerAction.MESSAGE) }
    val actions = buildList {
        if (state.contact.phoneNumbers.isNotEmpty()) {
            add(
                ContactActionsItem(
                    icon = R.drawable.call,
                    onClick = {
                        if (state.contact.phoneNumbers.size > 1) {
                            action = NumberPickerAction.CALL
                            showNumberPicker = true
                        } else {
                            val number = state.contact.phoneNumbers.first().number
                            onHandleContactDetailsAction(ContactDetailsAction.CallNumber(number))
                        }
                    }
                )
            )
            add(
                ContactActionsItem(
                    icon = R.drawable.messages_filled,
                    onClick = {
                        if (state.contact.phoneNumbers.size > 1) {
                            action = NumberPickerAction.MESSAGE
                            showNumberPicker = true
                        } else {
                            val threadId =
                                state.contact.phoneNumbers.first().number.getThreadIdOrCreate(
                                    context
                                )
                            onNavigate(Screen.ConversationDetails(threadId))
                        }
                    }
                )
            )
        }
//        add(
//            ContactActionsItem(
//                icon = if (state.contact.isFavorite) R.drawable.favorite_filled else R.drawable.favorite,
//                onClick = {
//                    onHandleContactDetailsAction(ContactDetailsAction.ToggleFavorite)
//                    onPlayFavoriteAnimation()
//                },
//                tint = if (state.contact.isFavorite) MaterialTheme.colorScheme.error else null
//            )
//        )

    }

    if (showNumberPicker) {
        NumberPickerDialog(
            onDismissRequest = { showNumberPicker = false },
            onPickNumber = { number ->
                showNumberPicker = false
                when (action) {
                    NumberPickerAction.MESSAGE -> {
                        val threadId = number.getThreadIdOrCreate(context)
                        onNavigate(Screen.ConversationDetails(threadId))
                    }

                    NumberPickerAction.CALL -> onHandleContactDetailsAction(ContactDetailsAction.CallNumber(number))
                }
            },
            phoneNumbers = state.contact.phoneNumbers.fastMap { it.number }
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