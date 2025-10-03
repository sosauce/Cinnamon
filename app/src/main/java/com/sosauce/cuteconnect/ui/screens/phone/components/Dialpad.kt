@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.phone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText

@Composable
fun Dialpad(
    onSendTone: (Char) -> Unit
) {

    var value by remember { mutableStateOf("") }
    val row1 = arrayOf('1', '2', '3')
    val row2 = arrayOf('4', '5', '6')
    val row3 = arrayOf('7', '8', '9')
    val row4 = arrayOf('*', '0', '#')
    val focusRequester = remember { FocusRequester() }

    //LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisableSoftKeyboard {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        ButtonGroup(
            overflowIndicator = {}
        ) {
            row1.forEach { number ->
                customItem(
                    {
                        IconButton(
                            onClick = {
                                value += number
                                onSendTone(number)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                        ) { CuteText(number.toString()) }
                    },
                    {}
                )
            }
        }
        ButtonGroup(
            overflowIndicator = {}
        ) {
            row2.forEach { number ->
                customItem(
                    {
                        IconButton(
                            onClick = {
                                value += number
                                onSendTone(number)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                        ) { CuteText(number.toString()) }
                    },
                    {}
                )
            }
        }
        ButtonGroup(
            overflowIndicator = {}
        ) {
            row3.forEach { number ->
                customItem(
                    {
                        IconButton(
                            onClick = {
                                value += number
                                onSendTone(number)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                        ) { CuteText(number.toString()) }
                    },
                    {}
                )
            }
        }
        ButtonGroup(
            overflowIndicator = {}
        ) {
            row4.forEach { number ->
                customItem(
                    {
                        IconButton(
                            onClick = {
                                value += number
                                onSendTone(number)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                        ) { CuteText(number.toString()) }
                    },
                    {}
                )
            }
        }
    }
}