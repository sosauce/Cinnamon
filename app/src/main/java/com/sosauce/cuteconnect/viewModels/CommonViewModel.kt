package com.sosauce.cuteconnect.viewModels

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cuteconnect.data.actions.CommonAction
import com.sosauce.cuteconnect.domain.model.CuteConversation
import com.sosauce.cuteconnect.domain.model.CuteMessage
import com.sosauce.cuteconnect.domain.model.CuteSimCard
import com.sosauce.cuteconnect.domain.repository.CommonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommonViewModel(
    private val commonRepository: CommonRepository
): ViewModel() {

//    val messages = commonRepository
//        .fetchLatestMessages()
//        .stateIn(
//            viewModelScope,
//            SharingStarted.WhileSubscribed(5000),
//            emptyList()
//        )

    val pinnedConversations = commonRepository
        .fetchLatestConversations()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val contacts = commonRepository
        .fetchLatestContacts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
         )

    val callLog = commonRepository
        .fetchLatestCallLog()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val voicemails = commonRepository
        .fetchLatestVoicemails()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    val simCards = commonRepository
        .fetchLatestSims()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )


    fun threadMessages(threadId: Long) = commonRepository.fetchLatestMessagesForThread(threadId)



    fun onHandleCommonAction(action: CommonAction) {
        when(action) {
            is CommonAction.SendMessage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    commonRepository.sendMessage(
                        address = action.cuteMessage.address,
                        message = action.cuteMessage.body
                    )
                    commonRepository.saveSmsToDevice(action.cuteMessage)
                }
            }
            is CommonAction.DeleteFromContentUri -> {
                viewModelScope.launch(Dispatchers.IO) {
                    commonRepository.deleteFromContentUri(
                        action.contentUri,
                        action.id
                    )
                }
            }

            is CommonAction.MarkMessageAsRead -> {
                viewModelScope.launch(Dispatchers.IO) {
                    commonRepository.markSmsAsRead(action.messageId)
                }
            }
        }

    }

    fun getOrCreateConversation(number: String) = commonRepository.getOrCreateConversation(number)


}