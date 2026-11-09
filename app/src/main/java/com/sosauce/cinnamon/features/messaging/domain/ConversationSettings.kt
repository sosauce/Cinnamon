package com.sosauce.cinnamon.features.messaging.domain

import android.net.Uri
import androidx.compose.ui.graphics.Color

data class ConversationSettings(
    val threadId: Long = 0,
    val draft: String = "",
    val wallpaper: Uri? = null,
    val wallpaperBlurIntensity: Int = 0,
    val color: Color? = null
)
