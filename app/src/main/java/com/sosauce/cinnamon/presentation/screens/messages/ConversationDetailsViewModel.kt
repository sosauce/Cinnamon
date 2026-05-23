@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.presentation.screens.messages

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.MediaStore
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
import com.sosauce.cinnamon.R
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
import com.sosauce.cinnamon.utils.beautifyNumber
import com.sosauce.cinnamon.utils.blockNumbers
import com.sosauce.cinnamon.utils.getContactNameOrNothing
import com.sosauce.cinnamon.utils.isShortCode
import com.sosauce.cinnamon.utils.observe
import com.sosauce.cinnamon.utils.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _state =
        MutableStateFlow(ConversationDetailsState(isLoading = true, threadId = threadId))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                conversationsRepository.fetchLatestSmsForThread(threadId),
                conversationsRepository.fetchLatestMmsForThread(threadId),
                scheduledMessagesDao.getScheduledMessagesForThread(threadId),
                application.contentResolver.observe(BlockedNumberContract.BlockedNumbers.CONTENT_URI)
            ) { sms, mms, scheduled, _ ->
                sms + mms + scheduled.fastMap { it.toCuteMessage() }
            }.collectLatest { messages ->
                _state.update {
                    it.copy(
                        messages = messages.sortedByDescending { it.date }
                            .groupBy { it.date.toDate() },
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val threadRecipients = conversationsRepository.fetchThreadRecipients(threadId)

            val isShortCode =
                if (threadRecipients.size > 1) false else threadRecipients.firstOrNull()
                    ?.isShortCode() == true

            _state.update {
                it.copy(
                    recipients = threadRecipients,
                    nameOrBeautifiedRecipients = threadRecipients.fastMap {
                        it.getContactNameOrNothing(
                            application
                        ).beautifyNumber()
                    },
                    isShortCode = isShortCode
                )
            }

            application.contentResolver.observe(BlockedNumberContract.BlockedNumbers.CONTENT_URI)
                .collectLatest {
                    if (state.value.recipients.size > 1) return@collectLatest

                    val number = state.value.recipients.firstOrNull() ?: return@collectLatest
                    _state.update {
                        it.copy(
                            isSoloRecipientBlocked = BlockedNumberContract.isBlocked(
                                application,
                                number
                            )
                        )
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            conversationSettingsDao.getConversationSettings(threadId).collectLatest { settings ->
                _state.update {
                    it.copy(settings = settings ?: ConversationSettings(threadId = threadId))
                }
            }
        }
    }

    fun deleteConversation() =
        viewModelScope.launch(Dispatchers.IO) { conversationsRepository.deleteConversation(threadId) }

    fun handleConversationSettingsActions(action: ConversationSettingActions) {
        when (action) {
            is ConversationSettingActions.UpsertConversationSettings -> {
                viewModelScope.launch(Dispatchers.IO) {
                    conversationSettingsDao.upsertConversation(action.conversationSettings)
                }
            }
        }
    }

    fun handleConversationActions(action: ConversationActions) {
        when (action) {
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
                    val scheduledMessageId =
                        scheduledMessagesDao.upsertScheduledMessage(action.scheduledMessage)
                    val delay = action.scheduledMessage.sendAt - System.currentTimeMillis()
                    val request = OneTimeWorkRequestBuilder<SendMessageWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(
                            workDataOf(
                                SendMessageWorker.SCHEDULED_MESSAGE_ID to scheduledMessageId
                            )
                        )
                        .build()
                    workManager.enqueueUniqueWork(
                        "Scheduled message ID: $scheduledMessageId",
                        ExistingWorkPolicy.KEEP,
                        request
                    )
                }
            }

            is ConversationActions.ClearThreadNotifications -> messageNotificationManager.clearThreadNotifications(
                threadId
            )

            is ConversationActions.ToggleBlock -> {
                viewModelScope.launch(Dispatchers.IO) {
                    if (state.value.recipients.size > 1) return@launch // can't block group chats (unless we block everyone but why)
                    val rawNumber = state.value.recipients.firstOrNull() ?: return@launch

                    if (state.value.isSoloRecipientBlocked) {
                        BlockedNumberContract.unblock(application, rawNumber)
                        _state.update {
                            it.copy(
                                isSoloRecipientBlocked = false
                            )
                        }
                    } else {
                        application.blockNumbers(listOf(rawNumber))
                    }

                }
            }

            is ConversationActions.DownloadMmsImage -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val contentValues = contentValuesOf(
                        MediaStore.Images.Media.DISPLAY_NAME to "mms_${System.currentTimeMillis()}.jpg",
                        MediaStore.Images.Media.MIME_TYPE to "image/jpeg",
                        MediaStore.Images.Media.IS_PENDING to 1,
                    )

                    val uri = application.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: return@launch

                    var bitmap: Bitmap? = null

                    application.contentResolver.openInputStream(action.image)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }

                    try {
                        if (bitmap == null) return@launch
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        application.contentResolver.update(uri, contentValues, null, null)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                application,
                                application.getString(R.string.saved),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        application.contentResolver.delete(uri, null, null)
                    }
                }
            }

            is ConversationActions.DeleteSelectedMessages -> {

                val scheduledMessages = action.messages.fastFilter { it.isScheduled }

                viewModelScope.launch(Dispatchers.IO) {
                    conversationsRepository.deleteMessages(
                        action.messages
                    )
                }

                if (scheduledMessages.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        scheduledMessages.fastMap { scheduledMessagesDao.getScheduledMessageById(it.id) }
                            .fastForEach {
                                scheduledMessagesDao.deleteScheduledMessage(it)
                                workManager.cancelUniqueWork("Scheduled message ID: ${it.id}")
                            }
                    }

                }
            }
        }
    }

}

/**
 * @param isSoloRecipientBlocked Always false for group chats
 */
data class ConversationDetailsState(
    val isLoading: Boolean = false,
    val threadId: Long = 0,
    val recipients: List<String> = emptyList(), // raw phone numbers in the conversation
    val nameOrBeautifiedRecipients: List<String> = emptyList(), // phone numbers or contacts in the conversation
    val settings: ConversationSettings = ConversationSettings(),
    val messages: Map<String, List<CuteMessage>> = emptyMap(),
    val isSoloRecipientBlocked: Boolean = false, // Always false for GCs
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
        val scheduledMessage: ScheduledMessage
    ) : ConversationActions

    data class DownloadMmsImage(val image: Uri) : ConversationActions

    data object ToggleBlock : ConversationActions

    data class DeleteSelectedMessages(
        val messages: List<CuteMessage>
    ) : ConversationActions

}