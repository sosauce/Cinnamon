package com.sosauce.cinnamon.features.contacts.presentation

import com.sosauce.cinnamon.features.contacts.domain.CuteContact

/**
 * Rows of the paged (non favorite) contacts list: contacts with a letter header before each group.
 */
sealed interface ContactsListItem {
    val key: String

    data class Header(
        val letter: Char,
        /**
         * Id of the first contact under this header, keeps keys unique if a letter shows up twice
         */
        val firstContactId: Long
    ) : ContactsListItem {
        override val key = "header_${letter}_$firstContactId"
    }

    data class Contact(val contact: CuteContact) : ContactsListItem {
        override val key = "contact_${contact.id}"
    }
}

val CuteContact.headerLetter: Char
    get() = displayName.firstOrNull()?.uppercaseChar() ?: '#'
