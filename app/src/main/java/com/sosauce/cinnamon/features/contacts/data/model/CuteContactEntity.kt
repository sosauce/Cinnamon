package com.sosauce.cinnamon.features.contacts.data.model

import com.sosauce.cinnamon.features.contacts.domain.ContactPhone

data class CuteContactEntity(
    val id: Long,
    val displayName: String,
    val thumbnail: String?,
    val isFavorite: Boolean,
    val accountName: String,
    val phoneNumbers: List<ContactPhone>
)
