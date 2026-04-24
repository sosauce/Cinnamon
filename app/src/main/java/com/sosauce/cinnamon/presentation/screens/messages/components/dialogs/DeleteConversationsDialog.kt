@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R

@Composable
fun DeleteConversationsDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    numberOfConversations: Int
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Image(
                painter = painterResource(R.drawable.delete_filled),
                contentDescription = null
            )
        },
        title = { Text("Delete conversations") },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.delete))
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
        text = {
            Text("Are you sure you want to delete $numberOfConversations conversations ? This cannot be undone!")
        }
    )
}