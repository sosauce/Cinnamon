package com.sosauce.cinnamon.core.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R

@Composable
fun BlockUnblockDialog(
    displayName: String,
    isBlocked: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmationText: String? = null,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
    val title = if (isBlocked) R.string.unblock else R.string.block
    val action = if (isBlocked) R.string.unblock else R.string.block
    val defaultConfirmation = if (isBlocked) {
        R.string.unblock_no_u_sure
    } else {
        R.string.block_are_u_sure
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.block),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(title)) },
        text = {
            Column {
                Text(confirmationText ?: stringResource(defaultConfirmation, displayName))
                additionalContent()
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(action))
            }
        }
    )
}
