package com.sosauce.cinnamon.features.phone.data.model

import com.sosauce.cinnamon.features.phone.domain.CuteSimCard

data class CuteCallLogEntity(
    val id: Long,
    val number: String,
    val cachedName: String?,
    val date: Long,
    val duration: Long,
    val location: String?,
    val presentation: Int,
    val type: Int,
    val photo: String?,
    val sim: CuteSimCard?
)
