package com.sosauce.cinnamon.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ConversationSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: Long = 0,
    val draft: String = "",
    val wallpaper: String = "", // Wallpaper's Uri as a String
    val wallpaperBlurIntensity: Int = 0,
    val color: Int = -1
)