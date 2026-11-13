package com.sosauce.cinnamon.features.contacts.data.model

/**
 * A slice of contacts plus the total amount of contacts matching the query.
 */
data class ContactsPage(
    val contacts: List<CuteContactEntity>,
    val totalCount: Int
)
