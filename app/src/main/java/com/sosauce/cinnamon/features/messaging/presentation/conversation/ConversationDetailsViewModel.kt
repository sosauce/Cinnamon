@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.MediaStore
import android.provider.Telephony
import android.widget.Toast
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.android.mms.util.DownloadManager
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.MediaManager
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingActions
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDao
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessageEntity
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessagesDao
import com.sosauce.cinnamon.features.messaging.data.repository.MessagesRepository
import com.sosauce.cinnamon.core.system.workers.SendMessageWorker
import com.sosauce.cinnamon.core.telephony.message.CuteTelephonyManager
import com.sosauce.cinnamon.core.telephony.message.MessageNotificationManager
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsEntity
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.cinnamon.core.utils.isShortCode
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.messaging.data.ScheduledMessageManager
import com.sosauce.cinnamon.features.messaging.data.model.toConversationSettings
import com.sosauce.cinnamon.features.messaging.data.model.toCuteConversation
import com.sosauce.cinnamon.features.messaging.data.model.toCuteMessage
import com.sosauce.cinnamon.features.messaging.data.model.toEntity
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.messaging.domain.ConversationSettings
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ConversationDetailsViewModel(
    private val application: Application,
    private val threadId: Long,
    private val messagesRepository: MessagesRepository,
    private val conversationsRepository: ConversationsRepository,
    private val conversationSettingsDao: ConversationSettingsDao,
    private val cuteTelephonyManager: CuteTelephonyManager,
    private val scheduledMessagesDao: ScheduledMessagesDao,
    private val scheduledMessageManager: ScheduledMessageManager,
    private val messageNotificationManager: MessageNotificationManager,
    private val mediaManager: MediaManager
) : AndroidViewModel(application) {


    private val messages = combine(
        messagesRepository.fetchLatestSmsForThread(threadId),
        messagesRepository.fetchLatestMmsForThread(threadId),
        scheduledMessageManager.getScheduledMessagesForThread(threadId)
    ) { sms, mms, scheduled ->
        (sms + mms + scheduled)
            .sortedByDescending { it.timestamp }
            .groupBy { it.date }
    }

    private val conversation = combine(
        conversationsRepository.fetchLatestConversationForThreadId(threadId),
        conversationSettingsDao.getDraftForThread(threadId)
    ) { conversation, draft ->
        conversation.toCuteConversation(draft.orEmpty())
    }
    private val settings = conversationSettingsDao
        .getConversationSettings(threadId)
        .mapLatest { (it ?: ConversationSettingsEntity(threadId = threadId)).toConversationSettings() }

    val state = combine(
        messages,
        conversation,
        settings
    ) { messages, conversation, settings ->
        ConversationDetailsState(
            messages = messages,
            conversation = conversation,
            settings = settings,
            isLoading = false,
            isShortCode = conversation.participants.firstOrNull()?.rawNumber?.isShortCode() ?: false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ConversationDetailsState(isLoading = true)
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
                    if (state.value.conversation.participants.size > 1) return@launch // can't block group chats (unless we block everyone but why)



                    val participantToBlock = state.value.conversation.participants.singleOrNull() ?: return@launch
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
        }
    }

}


data class ConversationDetailsState(
    val isLoading: Boolean = false,
    val conversation: CuteConversation = CuteConversation(),
    val settings: ConversationSettings = ConversationSettings(),
    val messages: Map<String, List<CuteMessage>> = emptyMap(),
    val isShortCode: Boolean = false
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
}

sealed interface ConversationDetailsEvents {
    data class MmsSave(val success: Boolean) : ConversationDetailsEvents
    data class Block(
        val success: Boolean,
        val numbers: List<String>
    ) : ConversationDetailsEvents
}