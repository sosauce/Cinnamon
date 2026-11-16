package com.sosauce.cinnamon.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import com.sosauce.cinnamon.settings.ContactsImportSession

@Composable
fun ContactsImportDialog(
    session: ContactsImportSession,
    onToggleContact: (key: Int, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStrategyChange: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit,
    onStartImport: () -> Unit
) {
    ImportDialog(
        title = stringResource(R.string.import_contacts),
        icon = R.drawable.migrate,
        session = session,
        previewSummary = { preview ->
            stringResource(R.string.import_preview_contacts, preview.size)
        },
        itemKey = { it.key },
        itemTitle = { it.displayName },
        itemSubtitle = { it.detailLine },
        onToggle = onToggleContact,
        onSelectAll = onSelectAll,
        onClearSelection = onClearSelection,
        onStrategyChange = onStrategyChange,
        onDismiss = onDismiss,
        onStartImport = onStartImport
    )
}
