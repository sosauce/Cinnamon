package com.sosauce.cinnamon.features.contacts.data.model

import androidx.core.net.toUri
import com.sosauce.cinnamon.features.contacts.domain.CuteContact

fun CuteContactEntity.toDomain(): CuteContact {
    return CuteContact(
        id = id,
        displayName = displayName,
        thumbnail = thumbnail?.toUri(),
        isFavorite = isFavorite,
        accountName = accountName,
        phoneNumbers = phoneNumbers
    )
}