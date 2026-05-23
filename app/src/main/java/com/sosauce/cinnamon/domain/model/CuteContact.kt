package com.sosauce.cinnamon.domain.model

import android.net.Uri
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.domain.UriSerializer
import com.sosauce.cinnamon.domain.model.CuteContact.Address
import com.sosauce.cinnamon.domain.model.CuteContact.Email
import com.sosauce.cinnamon.domain.model.CuteContact.Event
import com.sosauce.cinnamon.domain.model.CuteContact.Phone
import com.sosauce.cinnamon.domain.model.CuteContact.Website
import kotlinx.serialization.Serializable


@Serializable
data class CuteContactDetails(
    val phoneNumbers: List<Phone> = emptyList(),
    val emails: List<Email> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val websites: List<Website> = emptyList(),
    val events: List<Event> = emptyList(),
    val note: String? = null,
    val company: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = ""
)

@Serializable
data class CuteContact(
    val id: Long = 0,
    val displayName: String = "",
    val accountName: String? = null,
    val accountType: String? = null,
    @Serializable(with = UriSerializer::class)
    val photo: Uri = Uri.EMPTY,
    val isFavorite: Boolean = false,
    val details: CuteContactDetails = CuteContactDetails()
) {


    val hasInfos =
        details.phoneNumbers.isNotEmpty() || details.emails.isNotEmpty() || details.addresses.isNotEmpty()
    val hasAbout =
        details.websites.isNotEmpty() || details.note?.isNotEmpty() == true || details.events.isNotEmpty()

    val searchIndex: String by lazy {
        buildString {
            append(displayName)
            append(" ")
            details.phoneNumbers.fastForEach { append("${it.number} ") }
            details.emails.fastForEach { append("${it.email} ") }
            details.addresses.fastForEach { append("${it.address} ") }
            append(details.note)
            append(" ")
            details.websites.fastForEach { append("${it.website} ") }
        }
    }

    @Serializable
    data class Email(
        val email: String,
        val type: Int,
        val isDefault: Boolean,
        val isBlocked: Boolean = false
    )

    @Serializable
    data class Phone(
        val number: String,
        val type: Int,
        val isDefault: Boolean,
        val isBlocked: Boolean = false
    )

    @Serializable
    data class Address(
        val address: String,
        val type: Int,
        val isDefault: Boolean
    )

    @Serializable
    data class Website(
        val website: String
    )

    @Serializable
    data class Event(
        val date: String,
        val type: Int
    )

}

class CuteContactDetailsBuilder {
    var note: String? = null
    var company: String = ""
    var firstName: String = ""
    var middleName: String = ""
    var lastName: String = ""


    private val phoneNumbers = mutableListOf<Phone>()
    private val emails = mutableListOf<Email>()
    private val addresses = mutableListOf<Address>()
    private val websites = mutableListOf<Website>()
    private val events = mutableListOf<Event>()

    fun addPhoneNumber(phone: Phone) = phoneNumbers.add(phone)
    fun addEmail(email: Email) = emails.add(email)
    fun addAddress(address: Address) = addresses.add(address)
    fun addWebsite(website: Website) = websites.add(website)
    fun addEvent(event: Event) = events.add(event)


    fun build(): CuteContactDetails {
        return CuteContactDetails(
            phoneNumbers = phoneNumbers.toList(),
            emails = emails.toList(),
            addresses = addresses.toList(),
            websites = websites.toList(),
            events = events.toList(),
            note = note,
            company = company,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName
        )
    }


}
