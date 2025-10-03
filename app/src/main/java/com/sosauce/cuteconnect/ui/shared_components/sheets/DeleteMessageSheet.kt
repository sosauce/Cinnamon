@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.shared_components.sheets

import android.provider.Telephony
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.data.actions.CommonAction
import com.sosauce.cuteconnect.ui.shared_components.buttons.DangerButton
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText

@Composable
fun DeleteMessageSheet(
    selectedMessages: Int,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            CuteText(
                text = pluralStringResource(R.plurals.delete_msg, selectedMessages, selectedMessages),
                style = MaterialTheme.typography.headlineMediumEmphasized.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            CuteText(
                text = "$selectedMessages messages",
                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.height(10.dp))
            CuteText(
                text = pluralStringResource(R.plurals.delete_msg_u_sure, selectedMessages, selectedMessages),
                style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(30.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest
                ) {
                    CuteText(stringResource(R.string.cancel))
                }
                DangerButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDelete
                ) {
                    CuteText(stringResource(R.string.delete))
                }
            }
        }

    }
    
}