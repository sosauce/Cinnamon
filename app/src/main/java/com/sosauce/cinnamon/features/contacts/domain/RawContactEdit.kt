package com.sosauce.cinnamon.features.contacts.domain

import androidx.core.net.toUri

/**
 * Isolated editable snapshot of a single raw contact.
 */
data class RawContactEdit(
    val rawContactId: Long? = null,
    val contactId: Long = 0,
    val displayName: String = "",
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val company: String? = null,
    val note: String? = null,
    val photoString: String? = null,
    val isFavorite: Boolean = false,
    val phoneNumbers: List<ContactPhone> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val addresses: List<ContactAddress> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val websites: List<String> = emptyList()
) {
    val photo = photoString?.toUri()
}
