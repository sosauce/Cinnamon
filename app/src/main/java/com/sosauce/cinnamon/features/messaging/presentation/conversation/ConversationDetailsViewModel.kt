@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation

import android.app.Application
import android.net.Uri
import android.provider.BlockedNumberContract
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sosauce.cinnamon.features.messaging.data.model.SearchableMessage
import com.sosauce.cinnamon.features.messaging.data.repository.ThreadMessagesPagingSource
import com.sosauce.cinnamon.features.messaging.domain.listKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import com.sosauce.cinnamon.core.MediaManager
import com.sosauce.cinnamon.core.telephony.message.CuteTelephonyManager
import com.sosauce.cinnamon.core.telephony.message.MessageNotificationManager
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.core.utils.isShortCode
import com.sosauce.cinnamon.features.messaging.data.ScheduledMessageManager
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingActions
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDao
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsEntity
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessageEntity
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessagesDao
import com.sosauce.cinnamon.features.messaging.data.model.toConversationSettings
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.model.toEntity
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.data.repository.MessagesRepository
import com.sosauce.cinnamon.features.messaging.domain.ConversationSettings
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.domain.Participant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationDetailsViewModel(
    private val application: Application,
    private val threadId: Long,
    private val messagesRepository: MessagesRepository,
    private val conversationsRepository: ConversationsRepository,
    private val conversationSettingsDao: ConversationSettingsDao,
    private val cuteTelephonyManager: CuteTelephonyManager,
    private val callManager: CallManager,
    private val scheduledMessagesDao: ScheduledMessagesDao,
    private val scheduledMessageManager: ScheduledMessageManager,
    private val messageNotificationManager: MessageNotificationManager,
    private val mediaManager: MediaManager
) : AndroidViewModel(application) {


    private val pagingSourceFactory = InvalidatingPagingSourceFactory {
        ThreadMessagesPagingSource(messagesRepository, threadId)
    }

    /**
     * SMS + MMS of the thread, newest first, paged. Scheduled messages are not part of it,
     * see [ConversationDetailsState.scheduledMessages].
     */
    val messages: Flow<PagingData<CuteMessage>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE * 2,
            prefetchDistance = PAGE_SIZE / 2,
            enablePlaceholders = true,
            jumpThreshold = PAGE_SIZE * 3
        ),
        pagingSourceFactory = pagingSourceFactory
    ).flow.cachedIn(viewModelScope)

    /**
     * Images & videos of the thread, used by the about screen.
     */
    val media = messagesRepository.fetchLatestMediaForThread(threadId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val scheduledMessages = scheduledMessageManager
        .getScheduledMessagesForThread(threadId)
        .map { scheduled -> scheduled.sortedByDescending { it.timestamp } }

    val searchTextFieldState = TextFieldState()
    private var initialSearchQueryApplied = false
    private val _search = MutableStateFlow(ConversationSearchState())

    private val searchQuery = snapshotFlow { searchTextFieldState.text.toString().trim() }
        .debounce(300.milliseconds)
        .distinctUntilChanged()

    // Only loaded while search is opened, and kept up to date with new/deleted messages
    private val searchCorpus = _search
        .map { it.isActive }
        .distinctUntilChanged()
        .flatMapLatest { isActive ->
            if (isActive) {
                messagesRepository
                    .observeMessageChanges()
                    .mapLatest { messagesRepository.getSearchableMessages(threadId) }
            } else {
                flowOf(emptyList())
            }
        }

    init {
        viewModelScope.launch {
            // first emission is the initial value, the paging source is already fresh
            messagesRepository.observeMessageChanges().drop(1).collect {
                pagingSourceFactory.invalidate()
            }
        }

        viewModelScope.launch {
            combine(
                searchQuery,
                searchCorpus,
                scheduledMessages
            ) { query, corpus, scheduled ->
                query to findSearchMatches(query, corpus, scheduled)
            }
                .flowOn(Dispatchers.Default)
                .collect { (query, matches) ->
                    _search.update { search ->
                        if (!search.isActive) return@update search

                        val newIndex = if (query == search.query) {
                            // same query but messages changed, stay on the same message if possible
                            matches
                                .indexOfFirst { it.key == search.currentMatch?.key }
                                .takeIf { it >= 0 }
                                ?: search.currentIndex.coerceIn(0, (matches.size - 1).coerceAtLeast(0))
                        } else 0

                        search.copy(
                            query = query,
                            matches = matches,
                            currentIndex = newIndex
                        )
                    }
                }
        }
    }

    /**
     * Matches are ordered newest first and their index is the position in the conversation list,
     * which renders scheduled messages first and then the paged messages.
     */
    private fun findSearchMatches(
        query: String,
        corpus: List<SearchableMessage>,
        scheduled: List<CuteMessage>
    ): List<SearchMatch> {
        if (query.isBlank()) return emptyList()

        val matches = mutableListOf<SearchMatch>()

        scheduled.fastForEachIndexed { index, message ->
            if (message.body.contains(query, ignoreCase = true)) {
                matches.add(SearchMatch(key = message.listKey, listIndex = index))
            }
        }
        corpus.fastForEachIndexed { index, message ->
            if (message.text.contains(query, ignoreCase = true)) {
                matches.add(SearchMatch(key = message.key, listIndex = scheduled.size + index))
            }
        }

        return matches
    }


    private val settings = conversationSettingsDao
        .getConversationSettings(threadId)
        .mapLatest { (it ?: ConversationSettingsEntity(threadId = threadId)).toConversationSettings() }

    private val _state = MutableStateFlow(
        ConversationDetailsState(
            isLoading = true,
            threadId = threadId
        )
    )

    val state = combine(
        _state,
        scheduledMessages,
        conversationsRepository.fetchLatestParticipantsForThreadId(threadId),
        settings,
        _search
    ) { state, scheduledMessages, participants, settings, search ->
        state.copy(
            scheduledMessages = scheduledMessages,
            search = search,
            participants = participants,
            settings = settings,
            isLoading = false,
            isShortCode = participants.firstOrNull()?.rawNumber?.isShortCode() ?: false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _state.value
    )


    private val _events = Channel<ConversationDetailsEvents>()
    val events = _events.receiveAsFlow()


    fun deleteConversation() =
        viewModelScope.launch(Dispatchers.IO) { messagesRepository.deleteConversation(threadId) }

    fun handleConversationSettingsActions(action: ConversationSettingActions) {
        when (action) {
            is ConversationSettingActions.UpsertConversationSettings -> {
                viewModelScope.launch(Dispatchers.IO) {
                    conversationSettingsDao.upsertConversation(action.conversationSettings.toEntity())
                }
            }

            is ConversationSettingActions.UpsertDraft -> {
                viewModelScope.launch(Dispatchers.IO) {
                    conversationSettingsDao.upsertDraft(threadId, action.draft)
                }
            }
        }
    }

    fun handleConversationActions(action: ConversationActions) {
        when (action) {
            is ConversationActions.MarkAsRead -> {
                viewModelScope.launch {
                    conversationsRepository.markConversationAsRead(threadId)
                }
            }

            is ConversationActions.SendMessage -> {
                viewModelScope.launch {
                    cuteTelephonyManager.sendMessage(
                        addresses = action.addresses,
                        message = action.message,
                        attachments = action.attachments
                    )
                }
            }

            is ConversationActions.ScheduleMessage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    scheduledMessageManager.schedule(action.scheduledMessageEntity)
                }
            }

            is ConversationActions.ClearThreadNotifications -> messageNotificationManager.clearThreadNotifications(
                threadId
            )

            is ConversationActions.ToggleBlock -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (state.value.participants.size > 1) return@launch // can't block group chats (unless we block everyone but why)



                    val participantToBlock = state.value.participants.singleOrNull() ?: return@launch
                    val rawNumber = participantToBlock.rawNumber


                    if (participantToBlock.isBlocked) {
                        BlockedNumberContract.unblock(application, rawNumber)
                    } else {
                        val success = cuteTelephonyManager.blockNumbers(
                            numbers = listOf(rawNumber)
                        )
                        _events.trySend(
                            ConversationDetailsEvents.Block(success, listOf(rawNumber))
                        )
                    }

                }
            }

            is ConversationActions.DownloadMmsImage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val success = mediaManager.saveImageToDevice(action.image)
                    _events.trySend(
                        ConversationDetailsEvents.MmsSave(success)
                    )
                }
            }

            is ConversationActions.DeleteSelectedMessages -> {

                val scheduledMessages = action.messages.fastFilter { it.isScheduled }

                viewModelScope.launch(Dispatchers.IO) {
                    messagesRepository.deleteMessages(
                        action.messages
                    )
                    if (scheduledMessages.isNotEmpty()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            scheduledMessages.fastMap { scheduledMessagesDao.getScheduledMessageById(it.id) }
                                .fastForEach { scheduledMessageManager.delete(it) }
                        }

                    }
                }
            }
            is ConversationActions.CallNumber -> callManager.startCall(action.number)

            is ConversationActions.OpenSearch -> _search.update { it.copy(isActive = true) }

            is ConversationActions.OpenSearchWithQuery -> {
                // the nav entry re-runs this when coming back to the screen, only apply it once
                if (initialSearchQueryApplied) return
                initialSearchQueryApplied = true
                searchTextFieldState.setTextAndPlaceCursorAtEnd(action.query)
                _search.update { it.copy(isActive = true) }
            }

            is ConversationActions.CloseSearch -> {
                searchTextFieldState.clearText()
                _search.value = ConversationSearchState()
            }

            is ConversationActions.ShowOlderSearchMatch -> _search.update { search ->
                search.copy(
                    currentIndex = (search.currentIndex + 1).coerceAtMost((search.matches.size - 1).coerceAtLeast(0))
                )
            }

            is ConversationActions.ShowNewerSearchMatch -> _search.update { search ->
                search.copy(currentIndex = (search.currentIndex - 1).coerceAtLeast(0))
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 50
    }

}

@Stable
data class ConversationDetailsState(
    val isLoading: Boolean = false,
    val threadId: Long,
    val participants: List<Participant> = emptyList(),
    val settings: ConversationSettings = ConversationSettings(),
    val scheduledMessages: List<CuteMessage> = emptyList(),
    val search: ConversationSearchState = ConversationSearchState(),
    val isShortCode: Boolean = false
)

@Stable
data class ConversationSearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val matches: List<SearchMatch> = emptyList(),
    val currentIndex: Int = 0
) {
    val currentMatch: SearchMatch?
        get() = matches.getOrNull(currentIndex)

    val hasOlderMatch: Boolean
        get() = currentIndex < matches.lastIndex

    val hasNewerMatch: Boolean
        get() = currentIndex > 0
}

/**
 * @param key [listKey] of the matching message
 * @param listIndex index of the matching message in the conversation list (newest = 0)
 */
data class SearchMatch(
    val key: String,
    val listIndex: Int
)

sealed interface ConversationActions {
    data object MarkAsRead : ConversationActions
    data object ClearThreadNotifications : ConversationActions
    data class SendMessage(
        val addresses: List<String>,
        val message: String,
        val attachments: List<Uri>
    ) : ConversationActions

    data class ScheduleMessage(
        val scheduledMessageEntity: ScheduledMessageEntity
    ) : ConversationActions

    data class DownloadMmsImage(val image: Uri) : ConversationActions

    data object ToggleBlock : ConversationActions

    data class DeleteSelectedMessages(
        val messages: List<CuteMessage>
    ) : ConversationActions

    data class CallNumber(
        val number: String
    ) : ConversationActions

    data object OpenSearch : ConversationActions
    data class OpenSearchWithQuery(val query: String) : ConversationActions
    data object CloseSearch : ConversationActions
    data object ShowOlderSearchMatch : ConversationActions
    data object ShowNewerSearchMatch : ConversationActions
}

sealed interface ConversationDetailsEvents {
    data class MmsSave(val success: Boolean) : ConversationDetailsEvents
    data class Block(
        val success: Boolean,
        val numbers: List<String>
    ) : ConversationDetailsEvents
}