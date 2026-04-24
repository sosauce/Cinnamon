package com.sosauce.cinnamon.domain.model

import android.net.Uri

data class CuteCallLog(
    val id: Long,
    val rawNumber: String,
    val callType: Int,
    val date: Long,
    val duration: Long,
    val location: String?,
    val presentation: Int,
    val cachedName: String?,
    val cachedPicture: Uri
)