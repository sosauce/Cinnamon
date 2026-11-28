package com.sosauce.cinnamon.features.contacts.domain

data class ContactAccount(
    val name: String?,
    val type: String?
)

data class ContactGroup(
    val id: Long,
    val title: String,
    val account: ContactAccount,
    val isReadOnly: Boolean
)
