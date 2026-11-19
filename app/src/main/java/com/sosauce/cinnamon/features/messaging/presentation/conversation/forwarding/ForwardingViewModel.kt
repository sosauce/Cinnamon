@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.forwarding

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

class ForwardingViewModel(
    private val message: String,
    private val conversationsRepository: ConversationsRepository
) : ViewModel() {


    val textFieldState = TextFieldState()
    private val searchQuery = snapshotFlow { textFieldState.text }.debounce(250.milliseconds)

    val state = combine(
        conversationsRepository.fetchLatestConversations(),
        searchQuery
    ) { conversations, search ->
        val searched = conversations.fastFilter { it.participants.fastAny { it.displayName.contains(search, true) } }

        ForwardingState(
            isLoading = false,
            conversations = searched.fastMap { it.toCuteConversation() },
            messageToForward = message
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ForwardingState(
            isLoading = true
        )
    )

}

@Stable
data class ForwardingState(
    val isLoading: Boolean = false,
    val conversations: List<CuteConversation> = emptyList(),
    val messageToForward: String = ""
)