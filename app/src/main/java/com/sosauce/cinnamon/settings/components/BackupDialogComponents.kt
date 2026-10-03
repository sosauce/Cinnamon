@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy

/**
 * Shared backup progress UI. Item counts are trackable in both backup flows,
 * so [progress] carries real 0..1 progress; when null (e.g. right before the
 * first item completes) an indeterminate wavy indicator is shown instead.
 */
@Composable
fun BackupProgressIndicator(
    progress: Float?,
    label: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = label ?: stringResource(R.string.backup_exporting_generic),
            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun ImportStrategySelector(
    strategy: ImportStrategy,
    enabled: Boolean,
    onStrategyChange: (ImportStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.import_strategy_title),
            style = MaterialTheme.typography.titleSmallEmphasized
        )
        ImportStrategy.entries.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = strategy == option,
                    onClick = { onStrategyChange(option) },
                    enabled = enabled
                )
                Text(
                    text = stringResource(
                        when (option) {
                            ImportStrategy.SKIP_EXISTING -> R.string.import_skip_existing
                            ImportStrategy.IMPORT_ALL -> R.string.import_import_all
                        }
                    ),
                    style = MaterialTheme.typography.bodyMediumEmphasized
                )
            }
        }
    }
}
