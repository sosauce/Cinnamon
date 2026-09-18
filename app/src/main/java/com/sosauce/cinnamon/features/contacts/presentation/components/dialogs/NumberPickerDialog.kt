@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.items.CuteListItemDefaults
import com.sosauce.cinnamon.core.utils.beautifyNumber

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
                    CuteListItem(
                        onClick = { onPickNumber(number) },
                        shape = CuteListItemDefaults.getItemShape(index, phoneNumbers.count()),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        leadingContent = {
                            Text(
                                text = "${index + 1}.",
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    ) {
                        Text(number.beautifyNumber())
                    }
//                    DropdownMenuItem(
//                        onClick = { onPickNumber(number) },
//                        shape = MenuDefaults.getItemShape(index, phoneNumbers.lastIndex),
//                        colors = MenuDefaults.itemVibrantColors(
//                            color
//                        ),
//                        text = { Text(number.beautifyNumber()) },
//                        leadingIcon = {
//                            Text("${index + 1}.")
//                        }
//                    )
                }
            }
        }
    )
}