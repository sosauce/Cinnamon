package com.sosauce.cinnamon.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import com.sosauce.cinnamon.settings.SmsImportSession

@Composable
fun SmsImportDialog(
    session: SmsImportSession,
    onToggleConversation: (threadId: Long, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStrategyChange: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit,
    onStartImport: () -> Unit
) {
    ImportDialog(
        title = stringResource(R.string.import_messages),
        icon = R.drawable.migrate,
        session = session,
        previewSummary = { preview ->
            stringResource(
                R.string.import_preview_sms,
                preview.size,
                preview.sumOf { it.messages.size }
            )
        },
        itemKey = { it.threadId },
        itemTitle = { conversation ->
            conversation.participants.joinToString(", ") { it.displayName }
                .ifBlank { conversation.threadId.toString() }
        },
        itemSubtitle = { conversation ->
            stringResource(R.string.import_messages_count, conversation.messages.size)
        },
        onToggle = onToggleConversation,
        onSelectAll = onSelectAll,
        onClearSelection = onClearSelection,
        onStrategyChange = onStrategyChange,
        onDismiss = onDismiss,
        onStartImport = onStartImport
    )
}
