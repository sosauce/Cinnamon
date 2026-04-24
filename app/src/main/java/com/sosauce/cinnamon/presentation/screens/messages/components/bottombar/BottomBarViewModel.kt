package com.sosauce.cinnamon.presentation.screens.messages.components.bottombar

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.conversation_settings.ConversationSettingsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BottomBarViewModel(
    private val threadId: Long,
    private val conversationSettingsDao: ConversationSettingsDao
): ViewModel() {

    val textFieldState = TextFieldState()
    private val _state = MutableStateFlow(BottomBarState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            snapshotFlow { textFieldState.text }.collectLatest { message ->
                _state.update {
                    it.copy(message = message.toString())
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val draft = conversationSettingsDao.getDraftForThread(threadId) ?: ""
            textFieldState.edit {
                this.append(draft)
            }
        }
    }

    fun addAttachments(uris: List<Uri>) {
        _state.update {
            it.copy(
                attachments = it.attachments + uris
            )
        }
    }

    fun removeAttachment(uri: Uri) {
        _state.update {
            it.copy(
                attachments = it.attachments - uri
            )
        }
    }

    fun resetState() {
        textFieldState.clearText()
        _state.update { BottomBarState() }
    }

    fun removeScheduledTime() = _state.update { it.copy(scheduledTime = null) }


    fun setScheduledTime(time: Long) = _state.update { it.copy(scheduledTime = time) }

}

data class BottomBarState(
    val message: String = "",
    val attachments: List<Uri> = emptyList(),
    val scheduledTime: Long? = null
)