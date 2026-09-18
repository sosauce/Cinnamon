@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.archived

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationsAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedConversationsViewModel(
    private val userPreferences: UserPreferences,
    private val conversationsRepository: ConversationsRepository
) : ViewModel() {


    val state = combine(
        userPreferences.archivedConversations,
        conversationsRepository.fetchLatestConversations()
    ) { archived, conversations ->
        val archivedConversations = conversations.fastFilter { archived.contains(it.threadId.toString()) }

        ArchivedState(
            isLoading = false,
            conversations = archivedConversations.fastMap { it.toCuteConversation("") }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ArchivedState(isLoading = true)
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

data class ArchivedState(
    val isLoading: Boolean = false,
    val conversations: List<CuteConversation> = emptyList()
)