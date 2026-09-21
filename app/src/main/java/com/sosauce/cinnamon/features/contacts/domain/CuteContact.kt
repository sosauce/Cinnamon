package com.sosauce.cinnamon.features.contacts.domain

import android.net.Uri
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri

data class CuteContact(
    val id: Long = 0,
    val displayName: String = "",
    val thumbnail: Uri? = null,
    val isFavorite: Boolean = false,
    val accountName: String = "",
    val accountType: String? = null,
    val phoneNumbers: List<ContactPhone> = emptyList()
) {
    val searchIndex: String by lazy {
        buildString {
            append(displayName)
            append(" ")
            phoneNumbers.fastForEach { append("${it.number} ") }
        }
    }
}

data class CuteRawContact(
    val id: Long,
    val accountName: String
)

data class CuteContactDetails(
    val photoString: String? = null,
    val emails: List<ContactEmail> = emptyList(),
    val addresses: List<ContactAddress> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val websites: List<String> = emptyList(),
    val company: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val lastName: String? = null,
    val note: String? = null
) {
    val photo = photoString?.toUri()
}

data class ContactPhone(
    val number: String,
    val type: Int,
    val isDefault: Boolean,
    val isBlocked: Boolean = false
)

data class ContactEmail(
    val email: String,
    val type: Int,
    val isDefault: Boolean
)

data class ContactAddress(
    val address: String,
    val type: Int,
    val isDefault: Boolean
)

data class ContactEvent(
    val date: String,
    val type: Int
)

class CuteContactDetailsBuilder {
    private var note: String? = null
    private var company: String? = null
    private var firstName: String? = null
    private var middleName: String? = null
    private var lastName: String? = null

    private var photo: String? = null


    private val emails = mutableListOf<ContactEmail>()
    private val addresses = mutableListOf<ContactAddress>()
    private val websites = mutableListOf<String>()
    private val events = mutableListOf<ContactEvent>()

    fun addEmail(email: ContactEmail) = emails.add(email)
    fun addAddress(address: ContactAddress) = addresses.add(address)
    fun addWebsite(website: String) = websites.add(website)
    fun addEvent(event: ContactEvent) = events.add(event)

    fun setNote(contactNote: String?) {
        note = contactNote
    }
    fun setCompany(contactCompany: String?) {
        company = contactCompany
    }

    fun setFirstName(contactFirstName: String?) {
        firstName = contactFirstName
    }

    fun setMiddleName(contactMiddleName: String?) {
        middleName = contactMiddleName
    }

    fun setLastName(contactLastName: String?) {
        lastName = contactLastName
    }

    fun setPhoto(contactPhoto: String?) {
        photo = contactPhoto
    }


    fun build(): CuteContactDetails {
        return CuteContactDetails(
            emails = emails.toList(),
            addresses = addresses.toList(),
            websites = websites.toList(),
            events = events.toList(),
            note = note,
            company = company,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            photoString = photo
        )
    }
}
