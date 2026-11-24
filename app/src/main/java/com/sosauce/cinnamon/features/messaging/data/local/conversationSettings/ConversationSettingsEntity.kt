package com.sosauce.cinnamon.features.messaging.data.local.conversationSettings

import androidx.room.Entity
import androidx.room.PrimaryKey

// Reserved row holding the wallpaper/color used by global chats setting
const val GLOBAL_THREAD_ID = -1L

@Entity
data class ConversationSettingsEntity(
    @PrimaryKey
    val threadId: Long = 0,
    val draft: String = "",
    val wallpaper: String = "",
    val wallpaperBlurIntensity: Int = 0,
    val color: Int = -1
)