package com.sosauce.cinnamon.features.contacts.data.backup

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.ContactAddress
import com.sosauce.cinnamon.features.contacts.domain.ContactEmail
import com.sosauce.cinnamon.features.contacts.domain.ContactEvent
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.cinnamon.features.contacts.domain.RawContactEdit
import com.sosauce.cinnamon.features.messaging.data.backup.ImportStrategy
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.parameter.TelephoneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class ImportableContact(
    val key: Int,
    val displayName: String,
    val detailLine: String,
    val vcard: VCard
)

data class ContactsImportResult(
    val imported: Int,
    val skipped: Int
)

/**
 * Restores contacts from a `.vcf` file picked through SAF, parsed with
 * ez-vcard. Inserts go through [ContactsRepository.createOrEditContact], the
 * same batch path the contact editor uses, on [Dispatchers.IO] with
 * per-contact progress.
 *
 * Duplicate detection is approximate: phone numbers are compared digit-wise
 * (formatting differs between providers) and emails case-insensitively.
 */
class ContactsImportRepository(
    private val context: Context,
    private val contactsRepository: ContactsRepository
) {

    suspend fun parseVcf(uri: Uri): List<ImportableContact> = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasContactsReadPermission(context)) {
            throw SecurityException(context.getString(R.string.backup_needs_contacts_permission))
        }
        val vcards = context.contentResolver.openInputStream(uri)?.use { stream ->
            Ezvcard.parse(stream).all()
        } ?: error(context.getString(R.string.import_empty_file))

        vcards.mapIndexed { index, vcard ->
            val phones = vcard.telephoneNumbers.mapNotNull { it.text }
            val emails = vcard.emails.mapNotNull { it.value }
            val name = vcard.formattedName?.value?.takeIf { it.isNotBlank() }
                ?: listOfNotNull(
                    vcard.structuredName?.given,
                    vcard.structuredName?.family
                ).joinToString(" ").takeIf { it.isNotBlank() }
                ?: phones.firstOrNull()
                ?: emails.firstOrNull()
                ?: "Contact ${index + 1}"
            ImportableContact(
                key = index,
                displayName = name,
                detailLine = phones.firstOrNull()
                    ?: emails.firstOrNull()
                    ?: vcard.organization?.values?.firstOrNull().orEmpty(),
                vcard = vcard
            )
        }
    }

    suspend fun importContacts(
        items: List<ImportableContact>,
        strategy: ImportStrategy,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): ContactsImportResult = withContext(Dispatchers.IO) {
        val knownPhones =
            if (strategy == ImportStrategy.SKIP_EXISTING) existingNormalizedPhones() else emptySet()
        val knownEmails =
            if (strategy == ImportStrategy.SKIP_EXISTING) existingEmails() else emptySet()

        var imported = 0
        var skipped = 0

        items.forEachIndexed { index, item ->
            val edit = item.vcard.toRawContactEdit()
            val duplicate = strategy == ImportStrategy.SKIP_EXISTING && isDuplicate(
                edit,
                knownPhones,
                knownEmails
            )
            if (duplicate) {
                skipped++
            } else {
                val ok = contactsRepository.createOrEditContact(edit)
                if (!ok) error(context.getString(R.string.backup_failed))
                imported++
            }
            if (items.isNotEmpty()) onProgress(index + 1, items.size)
        }

        ContactsImportResult(imported = imported, skipped = skipped)
    }

    fun readDisplayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: context.getString(R.string.backup_file_name)
    }

    private fun isDuplicate(
        edit: RawContactEdit,
        knownPhones: Set<String>,
        knownEmails: Set<String>
    ): Boolean {
        if (edit.phoneNumbers.any { normalizePhone(it.number) in knownPhones }) return true
        if (edit.emails.any { it.email.trim().lowercase(Locale.US) in knownEmails }) return true
        return false
    }

    private fun existingNormalizedPhones(): Set<String> {
        val out = mutableSetOf<String>()
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER),
            null, null, null
        )?.use { cursor ->
            val col = cursor.getColumnIndexOrThrow(Phone.NUMBER)
            while (cursor.moveToNext()) {
                out.add(normalizePhone(cursor.getString(col).orEmpty()))
            }
        }
        return out
    }

    private fun existingEmails(): Set<String> {
        val out = mutableSetOf<String>()
        context.contentResolver.query(
            Email.CONTENT_URI,
            arrayOf(Email.ADDRESS),
            null, null, null
        )?.use { cursor ->
            val col = cursor.getColumnIndexOrThrow(Email.ADDRESS)
            while (cursor.moveToNext()) {
                out.add(cursor.getString(col).orEmpty().trim().lowercase(Locale.US))
            }
        }
        return out
    }

    private fun normalizePhone(number: String): String {
        val trimmed = number.trim()
        val prefix = if (trimmed.startsWith("+")) "+" else ""
        return prefix + trimmed.filter { it.isDigit() }
    }

    private fun VCard.toRawContactEdit(): RawContactEdit {
        val structuredName = structuredName
        val phones = telephoneNumbers.mapNotNull { tel ->
            val number = tel.text ?: return@mapNotNull null
            ContactPhone(
                number = number,
                type = tel.types.firstOrNull()?.let {
                    when (it) {
                        TelephoneType.CELL -> Phone.TYPE_MOBILE
                        TelephoneType.HOME -> Phone.TYPE_HOME
                        TelephoneType.WORK -> Phone.TYPE_WORK
                        TelephoneType.FAX -> Phone.TYPE_FAX_HOME
                        TelephoneType.PAGER -> Phone.TYPE_PAGER
                        else -> Phone.TYPE_OTHER
                    }
                } ?: Phone.TYPE_OTHER,
                isDefault = tel.types.contains(TelephoneType.PREF)
            )
        }
        val emails = this.emails.mapNotNull { email ->
            val address = email.value ?: return@mapNotNull null
            ContactEmail(
                email = address,
                type = email.types.firstOrNull()?.let {
                    when (it) {
                        ezvcard.parameter.EmailType.HOME -> Email.TYPE_HOME
                        ezvcard.parameter.EmailType.WORK -> Email.TYPE_WORK
                        else -> Email.TYPE_OTHER
                    }
                } ?: Email.TYPE_OTHER,
                isDefault = email.types.any { it == ezvcard.parameter.EmailType.PREF }
            )
        }
        val addresses = addresses.map { address ->
            ContactAddress(
                address = listOf(
                    address.streetAddresses.joinToString(" "),
                    address.locality,
                    address.region,
                    address.postalCode,
                    address.country
                ).filter { !it.isNullOrBlank() }.joinToString(", "),
                type = address.types.firstOrNull()?.let {
                    when (it) {
                        ezvcard.parameter.AddressType.HOME -> StructuredPostal.TYPE_HOME
                        ezvcard.parameter.AddressType.WORK -> StructuredPostal.TYPE_WORK
                        else -> StructuredPostal.TYPE_OTHER
                    }
                } ?: StructuredPostal.TYPE_OTHER,
                isDefault = address.types.any { it == ezvcard.parameter.AddressType.PREF }
            )
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val events = buildList {
            birthday?.let { birthday ->
                val date = birthday.date?.let { dateFormat.format(it) }
                    ?: birthday.partialDate?.toString()
                date?.let { add(ContactEvent(it, Event.TYPE_BIRTHDAY)) }
            }
            anniversary?.let { anniversary ->
                val date = anniversary.date?.let { dateFormat.format(it) }
                    ?: anniversary.partialDate?.toString()
                date?.let { add(ContactEvent(it, Event.TYPE_ANNIVERSARY)) }
            }
            extendedProperties
                .filter { it.propertyName.equals("X-ABDATE", ignoreCase = true) }
                .forEach { add(ContactEvent(it.value ?: "", Event.TYPE_OTHER)) }
        }

        return RawContactEdit(
            rawContactId = null,
            displayName = formattedName?.value.orEmpty(),
            firstName = structuredName?.given,
            middleName = structuredName?.additionalNames?.firstOrNull(),
            lastName = structuredName?.family,
            company = organization?.values?.firstOrNull(),
            note = notes.firstOrNull()?.value,
            photoString = writePhotoCache(),
            phoneNumbers = phones,
            emails = emails,
            addresses = addresses,
            events = events,
            websites = urls.mapNotNull { it.value }
        )
    }

    private fun VCard.writePhotoCache(): String? {
        val bytes = photos.firstOrNull()?.data ?: return null
        return runCatching {
            val file = File(context.cacheDir, "import_photo_${System.currentTimeMillis()}.jpg")
            file.writeBytes(bytes)
            file.toUri().toString()
        }.getOrNull()
    }
}
