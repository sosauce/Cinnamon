@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.contacts.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.utils.beautifyNumber
import com.sosauce.cinnamon.utils.getItemShape

@Composable
fun NumberPickerDialog(
    onDismissRequest: () -> Unit,
    onPickNumber: (String) -> Unit,
    phoneNumbers: List<String>
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Image(
                painter = painterResource(R.drawable.phone_filled),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.select_number)) },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            Column {
                phoneNumbers.fastForEachIndexed { index, number ->
                    DropdownMenuItem(
                        onClick = { onPickNumber(number) },
                        shape = MenuDefaults.getItemShape(index, phoneNumbers.lastIndex),
                        text = { Text(number.beautifyNumber()) },
                        leadingIcon = {
                            Text("${index + 1}.")
                        }
                    )
                }
            }
        }
    )
}