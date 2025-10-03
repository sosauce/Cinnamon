package com.sosauce.cuteconnect.domain.model

import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract

data class CuteContact(
    val id: Long,
    val name: String,
    val photo: Uri,
    val isFavorite: Boolean,
    val phoneNumbers: List<Phone>,
    val emails: List<Email>,
    val addresses: List<Address>,
    val websites: List<Website>,
    val notes: List<Note>,
    val events: List<Event>
) {
    data class Email(
        val email: String,
        val type: Int,
        val isDefault: Boolean
    )

    data class Phone(
        val number: String,
        val type: Int,
        val isDefault: Boolean
    )

    data class Address(
        val address: String,
        val type: Int,
        val isDefault: Boolean
    )

    data class Website(
        val website: String
    )

    data class Note(
        val note: String
    )

    data class Event(
        val date: String,
        val type: Int
    )

}
