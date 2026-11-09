@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsAction
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsState
import com.sosauce.cinnamon.features.contacts.presentation.components.dialogs.NumberPickerDialog


@Composable
fun ContactActionsRow(
    state: ContactDetailsState,
    onNavigate: (Screen) -> Unit,
    onHandleContactDetailsAction: (ContactDetailsAction) -> Unit,
    onPlayFavoriteAnimation: () -> Unit
) {

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val buttonHeight = ButtonDefaults.MediumContainerHeight
    var showNumberPicker by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf(NumberPickerAction.MESSAGE) }
    val hasNumbers = state.contact.phoneNumbers.isNotEmpty()

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

    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .height(buttonHeight)
    ) {
        if (hasNumbers) {
            FilledTonalButton(
                onClick = {
                    if (state.contact.phoneNumbers.size > 1) {
                        action = NumberPickerAction.MESSAGE
                        showNumberPicker = true
                    } else {
                        val threadId =
                            state.contact.phoneNumbers.first().number.getThreadIdOrCreate(context)
                        onNavigate(Screen.ConversationDetails(threadId))
                    }
                },
                shapes = ButtonShapes(
                    shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                    pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight)
            ) {
                Icon(
                    painter = painterResource(R.drawable.messages_filled),
                    contentDescription = stringResource(R.string.send_msg),
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize)
                )
            }
            val middle = ButtonGroupDefaults.connectedMiddleButtonShapes()
            Button(
                onClick = {
                    if (state.contact.phoneNumbers.size > 1) {
                        action = NumberPickerAction.CALL
                        showNumberPicker = true
                    } else {
                        val number = state.contact.phoneNumbers.first().number
                        onHandleContactDetailsAction(ContactDetailsAction.CallNumber(number))
                    }
                },
                shapes = ButtonShapes(
                    shape = middle.shape,
                    pressedShape = middle.pressedShape
                ),
                modifier = Modifier
                    .weight(2f)
                    .height(buttonHeight)
            ) {
                Icon(
                    painter = painterResource(R.drawable.call),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.call),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1
                )
            }
        }
        ToggleButton(
            checked = state.contact.isFavorite,
            onCheckedChange = {
                onHandleContactDetailsAction(ContactDetailsAction.ToggleFavorite)
                onPlayFavoriteAnimation()
            },
            shapes = if (hasNumbers) ButtonGroupDefaults.connectedTrailingButtonShapes() else ToggleButtonShapes(
                shape = CircleShape,
                pressedShape = RoundedCornerShape(12.dp),
                checkedShape = CircleShape
            ),
            colors = ToggleButtonColors(
                containerColor = colors.surfaceContainerHigh,
                contentColor = colors.onSurface,
                disabledContainerColor = colors.onSurface.copy(alpha = 0.1f),
                disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
                checkedContainerColor = colors.tertiary,
                checkedContentColor = colors.onTertiary
            ),
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
        ) {
            val icon = if (state.contact.isFavorite) R.drawable.favorite_filled else R.drawable.favorite
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.MediumIconSize)
            )
        }
    }
}

private enum class NumberPickerAction {
    MESSAGE,
    CALL
}