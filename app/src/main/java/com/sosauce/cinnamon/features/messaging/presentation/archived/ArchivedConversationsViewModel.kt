@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.messaging.presentation.archived

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationsAction
import com.sosauce.cinnamon.features.messaging.presentation.conversation.perform
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ArchivedConversationsViewModel(
    private val userPreferences: UserPreferences,
    private val conversationsRepository: ConversationsRepository
) : ViewModel() {


    val textFieldState = TextFieldState()
    private val searchQuery = snapshotFlow { textFieldState.text }.debounce(250.milliseconds)

    val state = combine(
        userPreferences.archivedConversations,
        conversationsRepository.fetchLatestConversations(),
        searchQuery
    ) { archived, conversations, search ->
        val archivedConversations =
            conversations.fastFilter { archived.contains(it.threadId.toString()) }
                .fastFilter { it.participants.fastAny { it.displayName.contains(search, true) } }

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
        viewModelScope.launch {
            action.perform(userPreferences, conversationsRepository)
        }
    }


}

@Stable
data class ArchivedState(
    val isLoading: Boolean = false,
    val conversations: List<CuteConversation> = emptyList()
)