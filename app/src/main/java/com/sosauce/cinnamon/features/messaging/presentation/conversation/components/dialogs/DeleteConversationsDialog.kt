@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.sosauce.cinnamon.R

/**
 * Confirmation before deleting several conversations at once
 */
@Composable
fun DeleteConversationsDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    numberOfConversations: Int
) {
    DeleteConfirmationDialog(
        icon = R.drawable.delete_filled,
        title = {
            Text(
                text = pluralStringResource(
                    R.plurals.plural_delete_conversations,
                    numberOfConversations,
                    numberOfConversations
                ),
                textAlign = TextAlign.Center
            )
        },
        text = pluralStringResource(R.plurals.delete_x_conversations_confirm, numberOfConversations, numberOfConversations),
        onDismissRequest = onDismissRequest,
        onDelete = onDelete
    )
}

/**
 * Confirmation before deleting the conversation that's currently open
 */
@Composable
fun DeleteConversationDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit
) {
    DeleteConfirmationDialog(
        icon = R.drawable.delete,
        title = { Text(stringResource(R.string.delete_convo)) },
        text = stringResource(R.string.delete_convo_u_sure),
        onDismissRequest = onDismissRequest,
        onDelete = onDelete
    )
}

@Composable
private fun DeleteConfirmationDialog(
    @DrawableRes icon: Int,
    title: @Composable () -> Unit,
    text: String,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null
            )
        },
        title = title,
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
        text = { Text(text) }
    )
}
