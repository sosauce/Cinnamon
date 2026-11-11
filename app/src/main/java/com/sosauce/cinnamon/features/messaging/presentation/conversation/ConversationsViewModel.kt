@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDao
import com.sosauce.cinnamon.features.messaging.data.model.matches
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ConversationsViewModel(
    private val conversationsRepository: ConversationsRepository,
    private val userPreferences: UserPreferences,
    private val conversationSettingsDao: ConversationSettingsDao
) : ViewModel() {

    val textFieldState = TextFieldState()

    private val searchQuery = snapshotFlow { textFieldState.text.toString() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val drafts = conversationSettingsDao.getAllDrafts()

    val state = combine(
        conversationsRepository.fetchLatestConversations(),
        userPreferences.pinnedConversations,
        drafts,
        userPreferences.archivedConversations,
        searchQuery
    ) { conversations, pinned, allDrafts, archived, query ->
        val visibleConversations = conversations
            .fastFilter { it.threadId.toString() !in archived && it.matches(query) }
            .fastMap { it.toCuteConversation(allDrafts[it.threadId].orEmpty()) }
        val (pinnedConversations, unpinnedConversations) = visibleConversations
            .partition { it.threadId.toString() in pinned }

        ConversationsState(
            isLoading = false,
            conversations = unpinnedConversations,
            pinnedConversations = pinnedConversations,
            hasArchivedThreads = archived.isNotEmpty()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ConversationsState(isLoading = true)
    )

    fun handleThreadsAction(action: ConversationsAction) {
        when (action) {
            is ConversationsAction.ArchiveConversations -> {
                viewModelScope.launch {
                    userPreferences.toggleArchiveThreads(action.threadIds)
                }
            }

            is ConversationsAction.PinConversations -> {
                viewModelScope.launch {
                    userPreferences.pinThreads(action.threadIds)
                }
            }

            is ConversationsAction.DeleteConversations -> {
                viewModelScope.launch {
                    conversationsRepository.deleteThreads(action.threadIds)
                }
            }
        }

    }


}

data class ConversationsState(
    val isLoading: Boolean = false,
    val hasArchivedThreads: Boolean = false,
    val conversations: List<CuteConversation> = emptyList(),
    val pinnedConversations: List<CuteConversation> = emptyList()
)

sealed interface ConversationsAction {
    data class ArchiveConversations(val threadIds: List<Long>) : ConversationsAction
    data class PinConversations(val threadIds: List<Long>) : ConversationsAction
    data class DeleteConversations(val threadIds: List<Long>) : ConversationsAction
}
