@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation

import android.provider.Telephony
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDao
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sosauce.cinnamon.features.messaging.data.model.CuteConversationEntity
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsPagingSource
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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

    /**
     * threadId -> amount of matching messages, null when not searching
     */
    private val searchResults: Flow<Map<Long, Int>?> = combine(
        searchQuery,
        userPreferences.searchMessageContent
    ) { query, searchMessageContent -> query.trim() to searchMessageContent }
        .distinctUntilChanged()
        .flatMapLatest { (query, searchMessageContent) ->
            if (query.isBlank()) {
                flowOf(null)
            } else {
                // refresh results when conversations change
                conversationsRepository
                    .observeConversationChanges()
                    .mapLatest { conversationsRepository.searchThreads(query, searchMessageContent) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val visiblePinnedIds = combine(
        userPreferences.pinnedConversations,
        userPreferences.archivedConversations
    ) { pinned, archived -> pinned - archived }
        .distinctUntilChanged()

    private val pinnedConversations = visiblePinnedIds.flatMapLatest { pinnedIds ->
        if (pinnedIds.isEmpty()) {
            flowOf(emptyList())
        } else {
            conversationsRepository.fetchLatestConversations(
                extraSelection = "${Telephony.Threads._ID} IN (${pinnedIds.toSqlIdList()})"
            )
        }
    }

    private var pagingSourceFactory: InvalidatingPagingSourceFactory<Int, CuteConversationEntity>? = null

    // true once the first page of conversations has been loaded, never goes back to false
    // so later refreshes (new messages, searching) keep the list visible
    private val conversationsLoaded = MutableStateFlow(false)

    /**
     * Unpinned & unarchived conversations, paged. While searching, only the matching ones.
     */
    val conversations: Flow<PagingData<CuteConversation>> = combine(
        userPreferences.pinnedConversations,
        userPreferences.archivedConversations,
        searchResults
    ) { pinned, archived, results ->
        val excludedIds = pinned + archived
        val selection = buildList {
            if (excludedIds.isNotEmpty()) {
                add("${Telephony.Threads._ID} NOT IN (${excludedIds.toSqlIdList()})")
            }
            if (results != null) {
                add(
                    if (results.isEmpty()) "0" // nothing matches
                    else "${Telephony.Threads._ID} IN (${results.keys.toSqlIdList()})"
                )
            }
        }.joinToString(" AND ").ifEmpty { null }

        selection to results
    }
        .distinctUntilChanged()
        .flatMapLatest { (selection, results) ->
            val factory = InvalidatingPagingSourceFactory {
                ConversationsPagingSource(
                    conversationsRepository = conversationsRepository,
                    extraSelection = selection,
                    onLoaded = { conversationsLoaded.value = true }
                )
            }
            pagingSourceFactory = factory

            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    initialLoadSize = PAGE_SIZE,
                    prefetchDistance = PAGE_SIZE / 2,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = factory
            ).flow.map { pagingData ->
                pagingData.map { entity ->
                    entity.toCuteConversation().copy(
                        matchingMessagesCount = results?.get(entity.threadId) ?: 0
                    )
                }
            }
        }
        .cachedIn(viewModelScope)
        .combine(drafts) { pagingData, allDrafts ->
            pagingData.map { it.copy(draft = allDrafts[it.threadId].orEmpty()) }
        }

    init {
        viewModelScope.launch {
            // first emission is the initial value, the paging source is already fresh
            conversationsRepository.observeConversationChanges().drop(1).collectLatest {
                pagingSourceFactory?.invalidate()
            }
        }
    }

    val state = combine(
        pinnedConversations,
        drafts,
        userPreferences.archivedConversations,
        searchResults,
        combine(searchQuery, conversationsLoaded, ::Pair)
    ) { pinnedConversations, allDrafts, archived, results, (query, loaded) ->
        ConversationsState(
            isLoading = !loaded,
            pinnedConversations = pinnedConversations
                .fastFilter { results == null || it.threadId in results }
                .fastMap {
                    val draft = allDrafts[it.threadId].orEmpty()

                    it.toCuteConversation(draft).copy(
                        matchingMessagesCount = results?.get(it.threadId) ?: 0
                    )
                },
            hasArchivedThreads = archived.isNotEmpty(),
            searchQuery = if (results != null) query.trim() else ""
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

    companion object {
        private const val PAGE_SIZE = 30
    }

}

@Stable
data class ConversationsState(
    val isLoading: Boolean = false,
    val hasArchivedThreads: Boolean = false,
    val pinnedConversations: List<CuteConversation> = emptyList(),
    val searchQuery: String = ""
)

/**
 * Thread ids are numeric, so they can be inlined safely (avoids SQLite's bound-variable limit)
 */
private fun Collection<Any>.toSqlIdList(): String =
    mapNotNull { it.toString().toLongOrNull() }.joinToString(",")

sealed interface ConversationsAction {
    data class ArchiveConversations(val threadIds: List<Long>) : ConversationsAction
    data class PinConversations(val threadIds: List<Long>) : ConversationsAction
    data class DeleteConversations(val threadIds: List<Long>) : ConversationsAction
}
