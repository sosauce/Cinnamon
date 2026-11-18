@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.items.CuteListItemDefaults
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.features.phone.domain.SimChoice
import com.sosauce.cinnamon.features.phone.domain.SimPickerState

/**
 * Asks which SIM to call with, when there are multiple SIMs and no default one.
 */
@Composable
fun SimPickerDialog(
    state: SimPickerState,
    onPickSim: (SimChoice) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Image(
                painter = painterResource(R.drawable.sim_card_filled),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.call_with)) },
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
                Text(
                    text = state.number.beautifyNumber(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                state.sims.fastForEachIndexed { index, sim ->
                    CuteListItem(
                        onClick = { onPickSim(sim) },
                        shape = CuteListItemDefaults.getItemShape(index, state.sims.count()),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.sim_card_filled),
                                contentDescription = null,
                                tint = Color(sim.color),
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    ) {
                        Text(sim.label.ifEmpty { stringResource(R.string.unknown) })
                    }
                }
            }
        }
    )
}
