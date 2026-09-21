@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.forwarding

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

class ForwardingViewModel(
    private val message: String,
    private val conversationsRepository: ConversationsRepository
) : ViewModel() {

    val state = conversationsRepository.fetchLatestConversations().mapLatest { conversations ->
        ForwardingState(
            isLoading = false,
            conversations = conversations.fastMap { it.toCuteConversation() },
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

data class ForwardingState(
    val isLoading: Boolean = false,
    val conversations: List<CuteConversation> = emptyList(),
    val messageToForward: String = ""
)