@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.customization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingActions
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDao
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsEntity
import com.sosauce.cinnamon.features.messaging.data.model.toConversationSettings
import com.sosauce.cinnamon.features.messaging.data.model.toEntity
import com.sosauce.cinnamon.features.messaging.domain.ConversationSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class ThemingViewModel(
    private val threadId: Long,
    private val conversationSettingsDao: ConversationSettingsDao
) : ViewModel() {

    val state = conversationSettingsDao.getConversationSettings(threadId).mapLatest {
        val settings = (it ?: ConversationSettingsEntity(threadId = threadId)).toConversationSettings()
        ThemingState(
            isLoading = false,
            settings = settings
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemingState(isLoading = true)
    )

    fun handleConversationSettingsActions(action: ConversationSettingActions) {
        when (action) {
            is ConversationSettingActions.UpsertConversationSettings -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val entity = action.conversationSettings.toEntity()
                    conversationSettingsDao.upsertConversation(entity)
                }
            }
        }
    }


}

data class ThemingState(
    val isLoading: Boolean = false,
    val settings: ConversationSettings = ConversationSettings()
)