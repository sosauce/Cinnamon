package com.sosauce.cinnamon.features.contacts.data.backup

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.compose.ui.util.fastFilter
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.contacts.data.model.CuteContactEntity
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import ezvcard.Ezvcard
import ezvcard.VCardVersion
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.ImageType
import ezvcard.parameter.TelephoneType
import ezvcard.property.Address
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.property.FormattedName
import ezvcard.property.Photo
import ezvcard.property.StructuredName
import ezvcard.property.Telephone
import ezvcard.property.Url
import ezvcard.util.PartialDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a standard multi-vCard (`.vcf`, vCard 3.0) snapshot of the selected
 * contacts and persists it through the Storage Access Framework [Uri].
 *
 * Photo bytes are embedded best-effort (skipped
 * when unreadable). Dates that cannot be parsed fall back to an
 * `X-ABDATE` extended property.
 */
class ContactsBackupRepository(
    private val context: Context,
    private val contactsRepository: ContactsRepository
) {

    suspend fun buildVcf(
        contactIds: Set<Long>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): String = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasContactsReadPermission(context)) {
            throw SecurityException(context.getString(R.string.backup_needs_contacts_permission))
        }

        val entities = contactsRepository.fetchContacts()
            .fastFilter { it.id in contactIds }

        val vcards = ArrayList<ezvcard.VCard>(entities.size)
        entities.forEachIndexed { index, entity ->
            val details = runCatching {
                contactsRepository.getContactDetailsOnce(entity.id)
            }.getOrNull()
            vcards.add(buildVCard(entity, details))
            if (entities.isNotEmpty()) {
                onProgress(index + 1, entities.size)
            }
        }

        Ezvcard.write(vcards).version(VCardVersion.V3_0).go()
    }

    suspend fun writeVcfToUri(destination: Uri, vcf: String) =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(destination, "wt")?.use { stream ->
                stream.write(vcf.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Unable to open output stream for backup destination")
        }

    fun defaultFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        return "contacts_${formatter.format(Date(nowMillis))}"
    }

    private fun buildVCard(entity: CuteContactEntity, details: CuteContactDetails?): ezvcard.VCard {
        val vcard = ezvcard.VCard()
        vcard.formattedName = FormattedName(entity.displayName)

        val structuredName = StructuredName()
        var hasStructuredName = false
        details?.firstName?.takeIf { it.isNotBlank() }?.let {
            structuredName.given = it
            hasStructuredName = true
        }
        details?.lastName?.takeIf { it.isNotBlank() }?.let {
            structuredName.family = it
            hasStructuredName = true
        }
        details?.middleName?.takeIf { it.isNotBlank() }?.let {
            structuredName.additionalNames.add(it)
            hasStructuredName = true
        }
        if (hasStructuredName) {
            vcard.structuredName = structuredName
        }

        entity.phoneNumbers.forEach { phone ->
            val telephone = Telephone(phone.number)
            mapPhoneType(phone.type)?.let { telephone.types.add(it) }
            vcard.addTelephoneNumber(telephone)
        }

        details?.emails?.forEach { email ->
            val property = ezvcard.property.Email(email.email)
            mapEmailType(email.type)?.let { property.types.add(it) }
            vcard.addEmail(property)
        }

        details?.addresses?.forEach { contactAddress ->
            val address = Address()
            address.streetAddress = contactAddress.address
            mapAddressType(contactAddress.type)?.let { address.types.add(it) }
            vcard.addAddress(address)
        }

        details?.websites?.forEach { website ->
            runCatching { vcard.addUrl(Url(website)) }
        }

        details?.note?.takeIf { it.isNotBlank() }?.let { vcard.addNote(it) }
        details?.company?.takeIf { it.isNotBlank() }?.let { vcard.setOrganization(it) }

        var birthdaySet = false
        var anniversarySet = false
        details?.events?.forEach { event ->
            when (event.type) {
                Event.TYPE_BIRTHDAY -> {
                    val parsed = runCatching { Birthday(PartialDate.parse(event.date)) }.getOrNull()
                    if (parsed != null && !birthdaySet) {
                        vcard.birthday = parsed
                        birthdaySet = true
                    } else {
                        vcard.addExtendedProperty("X-ABDATE", event.date)
                    }
                }

                Event.TYPE_ANNIVERSARY -> {
                    val parsed =
                        runCatching { Anniversary(PartialDate.parse(event.date)) }.getOrNull()
                    if (parsed != null && !anniversarySet) {
                        vcard.anniversary = parsed
                        anniversarySet = true
                    } else {
                        vcard.addExtendedProperty("X-ABDATE", event.date)
                    }
                }

                else -> vcard.addExtendedProperty("X-ABDATE", event.date)
            }
        }

        loadPhoto(details?.photoString)?.let { vcard.addPhoto(it) }

        return vcard
    }

    private fun loadPhoto(photoString: String?): Photo? {
        if (photoString.isNullOrBlank()) return null
        return runCatching {
            val uri = photoString.toUri()
            val imageType = when (context.contentResolver.getType(uri)) {
                "image/png" -> ImageType.PNG
                "image/gif" -> ImageType.GIF
                "image/jpeg", "image/jpg" -> ImageType.JPEG
                else -> return null
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                Photo(stream.readBytes(), imageType)
            }
        }.getOrNull()
    }

    private fun mapPhoneType(type: Int): TelephoneType? = when (type) {
        Phone.TYPE_MOBILE -> TelephoneType.CELL
        Phone.TYPE_HOME -> TelephoneType.HOME
        Phone.TYPE_WORK -> TelephoneType.WORK
        Phone.TYPE_FAX_HOME, Phone.TYPE_FAX_WORK -> TelephoneType.FAX
        Phone.TYPE_PAGER -> TelephoneType.PAGER
        else -> null
    }

    private fun mapEmailType(type: Int): EmailType? = when (type) {
        Email.TYPE_HOME -> EmailType.HOME
        Email.TYPE_WORK -> EmailType.WORK
        else -> null
    }

    private fun mapAddressType(type: Int): AddressType? = when (type) {
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> AddressType.HOME
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> AddressType.WORK
        else -> null
    }
}
