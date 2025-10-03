package com.sosauce.cuteconnect.ui.shared_components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText

/**
 * A delete dropdown menu item. Provides an alert dialog as one should always be shown before deleting anything.
 */
@Composable
fun DropdownItemDelete(
    onDelete: () -> Unit,
    dialogTitle: @Composable (() -> Unit)? = null,
    dialogText: @Composable (() -> Unit)? = null,
) {

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.delete)
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) { CuteText(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDialog = false
                    }
                ) { CuteText(stringResource(R.string.delete)) }
            },
            text = dialogText,
            title = dialogTitle
        )
    }

    CuteDropdownMenuItem(
        onClick = { showDialog = true },
        text = { CuteText(stringResource(R.string.delete)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.delete),
                contentDescription = stringResource(R.string.delete)
            )
        },
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error,
            leadingIconColor = MaterialTheme.colorScheme.error
        )
    )

}