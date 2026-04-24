@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.presentation.screens.messages

import android.provider.Telephony
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.conversation_settings.ConversationSettingsDao
import com.sosauce.cinnamon.data.datastore.UserPreferences
import com.sosauce.cinnamon.domain.model.CuteConversation
import com.sosauce.cinnamon.domain.repository.MessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val messagesRepository: MessagesRepository,
    private val userPreferences: UserPreferences,
    private val conversationSettingsDao: ConversationSettingsDao
): ViewModel() {

    private val _state = MutableStateFlow(ConversationsState(isLoading = true))
    val state = _state.asStateFlow()



    init {


        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.archivedConversations.flatMapLatest { archived ->
                val extraSelection = if (archived.isNotEmpty()) {
                    "${Telephony.Threads._ID} NOT IN (${archived.joinToString { "?" }})"
                } else { null }
                // never query archived convos in the first place
                combine(
                    messagesRepository.fetchLatestConversations(
                        extraSelection = extraSelection,
                        extraSelectionArgs = archived.toTypedArray()
                    ),
                    userPreferences.pinnedConversations,
                    conversationSettingsDao.getAllDrafts(),
                ) { cleanConversations, pinned, allDrafts ->
                    val (pinnedThreads, unpinnedThreads) = cleanConversations
                        .fastMap {
                            val draft = allDrafts[it.threadId] ?: ""
                            it.copy(draft = draft)
                        }
                        .partition { it.threadId.toString() in pinned }

                    ConversationsState(
                        isLoading = false,
                        conversations = unpinnedThreads,
                        pinnedConversations = pinnedThreads,
                        hasArchivedThreads = archived.isNotEmpty()
                    )
                }

            }.collectLatest { newState -> _state.update { newState } }
        }
    }

    fun handleThreadsAction(action: ConversationsAction) {
        when(action) {
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
    val pinnedConversations: List<CuteConversation> = emptyList()
)

sealed interface ConversationsAction {
    data class ArchiveConversations(val threadIds: List<Long>) : ConversationsAction
    data class PinConversations(val threadIds: List<Long>) : ConversationsAction
    data class DeleteConversations(val threadIds: List<Long>) : ConversationsAction
}
