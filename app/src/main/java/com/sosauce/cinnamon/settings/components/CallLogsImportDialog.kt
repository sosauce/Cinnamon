package com.sosauce.cinnamon.settings.components

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import com.sosauce.cinnamon.settings.CallLogsImportSession

@Composable
fun CallLogsImportDialog(
    session: CallLogsImportSession,
    onToggleLog: (key: Int, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStrategyChange: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit,
    onStartImport: () -> Unit
) {
    val context = LocalContext.current

    ImportDialog(
        title = stringResource(R.string.import_call_logs),
        icon = R.drawable.phone,
        session = session,
        previewSummary = { preview ->
            stringResource(R.string.import_preview_calls, preview.size)
        },
        itemKey = { it.key },
        itemTitle = { item ->
            val log = item.log
            log.cachedName?.takeIf { it.isNotBlank() }
                ?: log.number.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.private_number)
        },
        itemSubtitle = { item ->
            val log = item.log
            remember(log.dateMillis, log.durationSeconds) {
                val date = DateUtils.formatDateTime(
                    context,
                    log.dateMillis,
                    DateUtils.FORMAT_ABBREV_MONTH
                )
                if (log.durationSeconds > 0) {
                    "$date • ${DateUtils.formatElapsedTime(log.durationSeconds)}"
                } else {
                    date
                }
            }
        },
        onToggle = onToggleLog,
        onSelectAll = onSelectAll,
        onClearSelection = onClearSelection,
        onStrategyChange = onStrategyChange,
        onDismiss = onDismiss,
        onStartImport = onStartImport
    )
}
