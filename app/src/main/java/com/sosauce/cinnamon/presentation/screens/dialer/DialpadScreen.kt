@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.dialer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.contacts.groupedContactsList
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.screens.phone.components.DisableSoftKeyboard
import com.sosauce.cinnamon.presentation.shared_components.items.CuteListItem
import com.sosauce.cinnamon.utils.backspace
import com.sosauce.cinnamon.utils.rememberFocusRequester

@Composable
fun DialpadScreen(
    state: DialpadState,
    onNavigate: (Screen) -> Unit,
    onNavigateUp: () -> Unit,
    onHandleCallAction: (CallAction) -> Unit
) {

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {
        val textFieldState = rememberTextFieldState()
        val dialpadLayout = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )
        val focusRequester = rememberFocusRequester()
        var showMultiNumberSelection by remember { mutableStateOf(Pair(false, 0L)) }

        if (showMultiNumberSelection.first) {
            val contact = state.contacts.fastFirst { it.id == showMultiNumberSelection.second }

            AlertDialog(
                onDismissRequest = { showMultiNumberSelection = Pair(false, 0L) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.call),
                        contentDescription = null
                    )
                },
                title = {
                    Text("Select a number to call")
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { showMultiNumberSelection = Pair(false, 0L) },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        contact.details.phoneNumbers.fastForEachIndexed { index, number ->
                            CuteListItem(
                                onClick = { onHandleCallAction(CallAction.LaunchCall(number.number)) },
                                leadingContent = { Text("${index + 1}.") },
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                shape = when(index) {
                                    0 -> MenuDefaults.leadingItemShape
                                    contact.details.phoneNumbers.lastIndex -> MenuDefaults.trailingItemShape
                                    else -> MenuDefaults.middleItemShape
                                },
                            ) { Text(number.number) }
                        }
                    }
                }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateUp,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.back),
                                contentDescription = null
                            )
                        }
                        DisableSoftKeyboard {
                            OutlinedTextField(
                                state = textFieldState,
                                lineLimits = TextFieldLineLimits.SingleLine,
                                modifier = Modifier.focusRequester(focusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                )
                            )
                        }
                        IconButton(
                            onClick = { textFieldState.backspace() },
                            shapes = IconButtonDefaults.shapes(),
                            enabled = textFieldState.text.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.backspace),
                                contentDescription = null
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    ) {
                        dialpadLayout.fastForEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                rowKeys.fastForEach { number ->
                                    Button(
                                        onClick = {
                                            textFieldState.edit { insert(textFieldState.text.length, number) }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shapes = ButtonDefaults.shapes(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        )
                                    ) {
                                        Text(
                                            text = number,
                                            style = MaterialTheme.typography.bodyLargeEmphasized
                                        )
                                    }
                                }
                            }
                        }
                        FilledIconButton(
                            onClick = { onHandleCallAction(CallAction.LaunchCall(textFieldState.text.toString())) },
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterHorizontally)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                            enabled = textFieldState.text.isNotEmpty() || textFieldState.text.isNotBlank()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.call),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                groupedContactsList(
                    contacts = state.contacts.fastFilter { it.details.phoneNumbers.fastAny { number -> number.number.contains(textFieldState.text) } },
                    onContactClicked = { contact ->
                        if (contact.details.phoneNumbers.size > 1) {
                            showMultiNumberSelection = Pair(true, contact.id)
                        } else {
                            onHandleCallAction(CallAction.LaunchCall(contact.details.phoneNumbers.first().number))
                        }
                    }
                )
            }

        }

    }

}