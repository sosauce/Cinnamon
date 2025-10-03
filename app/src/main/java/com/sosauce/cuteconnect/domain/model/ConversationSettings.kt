package com.sosauce.cuteconnect.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ConversationSettings(
    @PrimaryKey(autoGenerate = false)
    val convoId: Long = 0,
    val draft: String = "",
    val isPinned: Boolean = false,
    val wallpaper: String = "", // Wallpaper's Uri as a String
    val wallpaperBlurIntensity: Int = 0,
    val allWallpapers: List<String> = emptyList(),
    val color: Int = 0,
    val allColors: List<Int> = emptyList()
)