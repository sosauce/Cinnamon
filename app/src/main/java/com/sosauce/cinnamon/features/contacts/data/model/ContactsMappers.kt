package com.sosauce.cinnamon.features.contacts.data.model

import androidx.core.net.toUri
import com.sosauce.cinnamon.features.contacts.domain.CuteContact2

fun CuteContactEntity.toDomain(): CuteContact2 {
    return CuteContact2(
        id = id,
        displayName = displayName,
        thumbnail = thumbnail?.toUri(),
        isFavorite = isFavorite,
        accountName = accountName,
        phoneNumbers = phoneNumbers
    )
}