@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.presentation.screens.messages

import android.app.Application
import android.provider.BlockedNumberContract
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.conversation_settings.ConversationSettingsDao
import com.sosauce.cinnamon.data.datastore.UserPreferences
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.domain.repository.MessagesRepository
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class ConversationsViewModel(
    private val application: Application,
    private val messagesRepository: MessagesRepository,
    private val userPreferences: UserPreferences,
    private val conversationSettingsDao: ConversationSettingsDao
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ConversationsState(isLoading = true))
    val state = _state.asStateFlow()

    private val textFieldState = TextFieldState()


    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                messagesRepository.fetchLatestConversations(),
                userPreferences.pinnedConversations,
                conversationSettingsDao.getAllDrafts(),
                userPreferences.archivedConversations,
                snapshotFlow { textFieldState.text }.debounce(250)
            ) { cleanConversations, pinned, allDrafts, archived, searchQuery ->
                val (pinnedThreads, unpinnedThreads) = cleanConversations
                    .fastFilter { it.threadId.toString() !in archived }
                    .fastFilter {
                        it.snippet.contains(searchQuery, true)
                    }
                    .fastMap {
                        val draft = allDrafts[it.threadId] ?: ""
                        it.copy(draft = draft)
                    }
                    .partition { it.threadId.toString() in pinned }

                ConversationsState(
                    isLoading = false,
                    conversations = unpinnedThreads,
                    pinnedConversations = pinnedThreads,
                    hasArchivedThreads = archived.isNotEmpty(),
                    textFieldState = textFieldState
                )
            }.collectLatest { newState -> _state.update { newState } }
        }

        viewModelScope.launch {
            application.contentResolver.observe(BlockedNumberContract.BlockedNumbers.CONTENT_URI)
                .onEach {

                    val newConversations = state.value.conversations.fastMap {
                        if (it.isGroupChat) {
                            it
                        } else {
                            val recipient = it.rawRecipients.firstOrNull()
                            if (recipient == null) {
                                it
                            } else {
                                it.copy(
                                    isSenderBlocked = BlockedNumberContract.isBlocked(
                                        application,
                                        recipient
                                    )
                                )
                            }
                        }
                    }

                    _state.update {
                        it.copy(
                            conversations = newConversations
                        )
                    }
                }.launchIn(viewModelScope + Dispatchers.IO)
        }
    }

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
                    messagesRepository.deleteThreads(action.threadIds)
                }
            }
        }

    }


}

data class ConversationsState(
    val isLoading: Boolean = false,
    val hasArchivedThreads: Boolean = false,
    val conversations: List<CuteConversation> = emptyList(),
    val pinnedConversations: List<CuteConversation> = emptyList(),
    val textFieldState: TextFieldState = TextFieldState()
)

sealed interface ConversationsAction {
    data class ArchiveConversations(val threadIds: List<Long>) : ConversationsAction
    data class PinConversations(val threadIds: List<Long>) : ConversationsAction
    data class DeleteConversations(val threadIds: List<Long>) : ConversationsAction
}
