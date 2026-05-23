@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.dialer

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.insert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.contacts.ContactListItem
import com.sosauce.cinnamon.presentation.screens.contacts.components.dialogs.NumberPickerDialog
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.screens.phone.components.DisableSoftKeyboard
import com.sosauce.cinnamon.presentation.shared_components.NoXFound
import com.sosauce.cinnamon.presentation.shared_components.buttons.LongClickButton
import com.sosauce.cinnamon.utils.LazyListKeys
import com.sosauce.cinnamon.utils.backspace
import com.sosauce.cinnamon.utils.rememberFocusRequester

@Composable
fun SharedTransitionScope.DialpadScreen(
    state: DialpadState,
    onNavigate: (Screen) -> Unit,
    onNavigateUp: () -> Unit,
    onHandleCallAction: (CallAction) -> Unit,
    onAddPlus: () -> Unit
) {
    val dialpadLayout = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )
    val t9Map = mapOf(
        "2" to "ABC",
        "3" to "DEF",
        "4" to "GHI",
        "5" to "JKL",
        "6" to "MNO",
        "7" to "PQRS",
        "8" to "TUV",
        "9" to "WXYZ",
        "0" to "+"
    )
    val focusRequester = rememberFocusRequester()
    var showMultiNumberSelection by remember { mutableStateOf(Pair(false, 0L)) }

    if (showMultiNumberSelection.first) {
        val contact = state.contacts.fastFirst { it.id == showMultiNumberSelection.second }

        NumberPickerDialog(
            onDismissRequest = { showMultiNumberSelection = Pair(false, 0) },
            onPickNumber = { number ->
                showMultiNumberSelection = Pair(false, 0)
                onHandleCallAction(CallAction.LaunchCall(contact.details.phoneNumbers.first().number))
            },
            phoneNumbers = contact.details.phoneNumbers.fastMap { it.number }
        )
    }


    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .navigationBarsPadding()
            ) {
                DisableSoftKeyboard {
                    OutlinedTextField(
                        state = state.textFieldState,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        leadingIcon = {
                            IconButton(
                                onClick = onNavigateUp,
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.back),
                                    contentDescription = null
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { state.textFieldState.backspace() },
                                shapes = IconButtonDefaults.shapes(),
                                enabled = state.textFieldState.text.isNotEmpty()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.backspace),
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    ) {
                        dialpadLayout.fastForEach { rowKeys ->
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                rowKeys.fastForEach { number ->
                                    val letters = t9Map[number] ?: ""
                                    val onLongClick = remember {
                                        if (letters == "+") {
                                            onAddPlus
                                        } else null
                                    }

                                    LongClickButton(
                                        onClick = {
                                            state.textFieldState.edit {
                                                insert(state.textFieldState.text.length, number)
                                            }
                                        },
                                        onLongClick = onLongClick,
                                        modifier = Modifier
                                            //.aspectRatio(18f / 9f)
                                            .size(
                                                IconButtonDefaults.mediumContainerSize(
                                                    IconButtonDefaults.IconButtonWidthOption.Wide
                                                )
                                            )
                                            .weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = number,
                                                style = MaterialTheme.typography.bodyLargeEmphasized
                                            )
                                            Text(
                                                text = letters,
                                                style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        FilledIconButton(
                            onClick = { onHandleCallAction(CallAction.LaunchCall(state.textFieldState.text.toString())) },
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterHorizontally)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                            enabled = state.textFieldState.text.isNotEmpty() || state.textFieldState.text.isNotBlank()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.call),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                if (state.contacts.isNotEmpty()) {

                    val (favorites, nonFavorites) = state.contacts.partition { it.isFavorite }

                    if (favorites.isNotEmpty()) {
                        item(LazyListKeys.FAVORITE_CONTACTS) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.favorite_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.favorites,
                                        favorites.size
                                    ),
                                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        items(
                            items = favorites,
                            key = { contact -> contact.id }
                        ) { contact ->

                            ContactListItem(
                                modifier = Modifier.animateItem(),
                                contact = contact,
                                isSelected = false,
                                onClick = {
                                    if (contact.details.phoneNumbers.size > 1) {
                                        showMultiNumberSelection = Pair(true, contact.id)
                                    } else {
                                        onHandleCallAction(CallAction.LaunchCall(contact.details.phoneNumbers.first().number))
                                    }
                                },
                                showNumber = false
                            )
                        }
                    }


                    nonFavorites.groupBy { it.displayName.firstOrNull()?.uppercaseChar() ?: '#' }
                        .toSortedMap().forEach { (letter, contacts) ->
                        item {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                        items(
                            items = contacts,
                            key = { contact -> contact.id }
                        ) { contact ->


                            ContactListItem(
                                modifier = Modifier.animateItem(),
                                contact = contact,
                                isSelected = false,
                                onClick = {
                                    if (contact.details.phoneNumbers.size > 1) {
                                        showMultiNumberSelection = Pair(true, contact.id)
                                    } else {
                                        onHandleCallAction(CallAction.LaunchCall(contact.details.phoneNumbers.first().number))
                                    }
                                },
                                showNumber = false
                            )
                        }
                    }
                } else {
                    item {
                        NoXFound(
                            headlineText = R.string.no_contacts_found,
                            bodyText = R.string.no_contacts_found_desc,
                            icon = R.drawable.contacts
                        )
                    }
                }
            }
        }

    }

}