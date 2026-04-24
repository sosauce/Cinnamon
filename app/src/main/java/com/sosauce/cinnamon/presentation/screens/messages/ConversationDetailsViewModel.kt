package com.sosauce.cinnamon.presentation.screens.messages

import android.app.Application
import android.net.Uri
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sosauce.cinnamon.data.conversation_settings.ConversationSettingActions
import com.sosauce.cinnamon.data.conversation_settings.ConversationSettingsDao
import com.sosauce.cinnamon.data.managers.MessageNotificationManager
import com.sosauce.cinnamon.data.schedulers.SendMessageWorker
import com.sosauce.cinnamon.data.schedulers.scheduled_messages.ScheduledMessage
import com.sosauce.cinnamon.data.schedulers.scheduled_messages.ScheduledMessagesDao
import com.sosauce.cinnamon.data.schedulers.scheduled_messages.toCuteMessage
import com.sosauce.cinnamon.data.telephony.CuteTelephonyManager
import com.sosauce.cinnamon.domain.model.ConversationSettings
import com.sosauce.cinnamon.domain.model.CuteMessage
import com.sosauce.cinnamon.domain.repository.ConversationsRepository
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.SandwichPosition
import com.sosauce.cinnamon.utils.beautifyNumber
import com.sosauce.cinnamon.utils.getContactNameOrNothing
import com.sosauce.cinnamon.utils.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.TimeUnit

class ConversationDetailsViewModel(
    private val application: Application,
    private val threadId: Long,
    private val conversationsRepository: ConversationsRepository,
    private val conversationSettingsDao: ConversationSettingsDao,
    private val cuteTelephonyManager: CuteTelephonyManager,
    private val scheduledMessagesDao: ScheduledMessagesDao,
    private val workManager: WorkManager,
    private val messageNotificationManager: MessageNotificationManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ConversationDetailsState(isLoading = true, threadId = threadId))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                launch {
                    combine(
                        conversationsRepository.fetchLatestSmsForThread(threadId),
                        conversationsRepository.fetchLatestMmsForThread(threadId),
                        scheduledMessagesDao.getScheduledMessagesForThread(threadId),
                    ) { sms, mms, scheduled -> sms + mms + scheduled.fastMap { it.toCuteMessage() } }.collectLatest { messages ->
                        _state.update {
                            it.copy(
                                messages = messages.sortedByDescending { it.date },
                                isLoading = false
                            )
                        }
                    }
                }
                launch {
                    val threadRecipients = conversationsRepository.fetchThreadRecipients(threadId)

                    _state.update {
                        it.copy(
                            recipients = threadRecipients,
                            nameOrBeautifiedRecipients = threadRecipients.fastMap { it.getContactNameOrNothing(application).beautifyNumber() }
                        )
                    }
                }
                launch {
                    conversationSettingsDao.getConversationSettings(threadId).collectLatest { settings ->
                        _state.update {
                            it.copy(settings = settings ?: ConversationSettings(threadId = threadId))
                        }
                    }
                }
            }

        }
    }

    fun deleteConversation() = viewModelScope.launch(Dispatchers.IO) { conversationsRepository.deleteConversation(threadId) }

    fun handleConversationSettingsActions(action: ConversationSettingActions) {
        when(action) {
            is ConversationSettingActions.UpsertConversationSettings -> {
                viewModelScope.launch(Dispatchers.IO) {
                    conversationSettingsDao.upsertConversation(action.conversationSettings)
                }
            }
        }
    }

    fun handleConversationActions(action: ConversationActions) {
        when(action) {
            is ConversationActions.MarkAsRead -> {
                viewModelScope.launch {
                    cuteTelephonyManager.markConversationAsRead(threadId)
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
                    val scheduledMessageId = scheduledMessagesDao.upsertScheduledMessage(action.scheduledMessage)
                    val delay = action.scheduledMessage.sendAt - System.currentTimeMillis()
                    val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(
                            workDataOf(
                                SendMessageWorker.SCHEDULED_MESSAGE_ID to scheduledMessageId
                            )
                        )
                        .build()
                    workManager.enqueue(request)
                }
            }

            is ConversationActions.ClearThreadNotifications -> messageNotificationManager.clearThreadNotifications(threadId)
        }
    }

}

data class ConversationDetailsState(
    val isLoading: Boolean = false,
    val threadId: Long = 0,
    val recipients: List<String> = emptyList(), // raw phone numbers in the conversation
    val nameOrBeautifiedRecipients: List<String> = emptyList(), // phone numbers in the conversation
    val settings: ConversationSettings = ConversationSettings(),
    val messages: List<CuteMessage> = emptyList()
)

sealed class ConversationActions {
    data object MarkAsRead : ConversationActions()
    data object ClearThreadNotifications : ConversationActions()
    data class SendMessage(
        val addresses: List<String>,
        val message: String,
        val attachments: List<Uri>
    ) : ConversationActions()

    data class ScheduleMessage(
        val scheduledMessage: ScheduledMessage
    ) : ConversationActions()
}