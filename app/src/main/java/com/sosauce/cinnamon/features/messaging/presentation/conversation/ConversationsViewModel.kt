@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDao
import com.sosauce.cinnamon.features.messaging.data.model.matches
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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

    private val drafts = conversationSettingsDao.getAllDrafts()

    val conversations: Flow<PagingData<CuteConversation>> = combine(
        userPreferences.pinnedConversations,
        userPreferences.archivedConversations,
        searchQuery
    ) { pinned, archived, query ->
        query to (pinned + archived).toThreadIds()
    }
        .distinctUntilChanged()
        .flatMapLatest { (query, excluded) ->
            conversationsRepository.pagedConversations(query, excluded)
        }
        .cachedIn(viewModelScope)
        .combine(drafts) { pagingData, allDrafts ->
            pagingData.map { it.toCuteConversation(allDrafts[it.threadId]) }
        }

    private val pinnedConversations = combine(
        userPreferences.pinnedConversations,
        userPreferences.archivedConversations
    ) { pinned, archived -> (pinned - archived).toThreadIds() }
        .distinctUntilChanged()
        .flatMapLatest { conversationsRepository.fetchLatestConversationsForThreadIds(it) }

    val state = combine(
        pinnedConversations,
        drafts,
        userPreferences.archivedConversations,
        searchQuery
    ) { pinned, allDrafts, archived, query ->
        ConversationsState(
            isLoading = false,
            pinnedConversations = pinned
                .fastFilter { it.matches(query) }
                .fastMap { it.toCuteConversation(allDrafts[it.threadId]) },
            hasArchivedThreads = archived.isNotEmpty()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ConversationsState(isLoading = true)
    )

    private fun Set<String>.toThreadIds() = mapNotNull { it.toLongOrNull() }.toSet()

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
    val pinnedConversations: List<CuteConversation> = emptyList()
)

sealed interface ConversationsAction {
    data class ArchiveConversations(val threadIds: List<Long>) : ConversationsAction
    data class PinConversations(val threadIds: List<Long>) : ConversationsAction
    data class DeleteConversations(val threadIds: List<Long>) : ConversationsAction
}
