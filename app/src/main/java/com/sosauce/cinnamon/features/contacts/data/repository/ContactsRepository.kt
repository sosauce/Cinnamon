@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.contacts.data.repository

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.contacts.data.model.CuteContactEntity
import com.sosauce.cinnamon.features.contacts.data.model.toDomain
import com.sosauce.cinnamon.features.contacts.domain.ContactAddress
import com.sosauce.cinnamon.features.contacts.domain.ContactEmail
import com.sosauce.cinnamon.features.contacts.domain.ContactEvent
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetailsBuilder
import com.sosauce.cinnamon.features.contacts.domain.CuteRawContact
import com.sosauce.cinnamon.features.contacts.domain.RawContactEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val context: Context
) {



    fun fetchLatestContacts() =
        context.contentResolver
            .observe(ContactsContract.Contacts.CONTENT_URI)
            .mapLatest {
                fetchContacts().fastMap { it.toDomain() }
            }
            .flowOn(Dispatchers.IO)

    fun fetchLatestContactsDetails(contactId: Long) =
        context.contentResolver
            .observe(ContactsContract.Data.CONTENT_URI)
            .mapLatest {
                fetchContactDetails(contactId)
            }
            .flowOn(Dispatchers.IO)

    fun fetchDialpadContacts(): List<CuteContact> =
        fetchContacts(
            extraSelection = "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = ?",
            extraSelectionArgs = arrayOf("1")
        ).fastMap { it.toDomain() }

    fun fetchContact(contactId: Long) =
        context.contentResolver
            .observe(ContactsContract.Contacts.CONTENT_URI)
            .mapLatest {
                fetchContacts(
                    extraSelection = "${ContactsContract.Contacts._ID} = ?",
                    extraSelectionArgs = arrayOf(contactId.toString())
                ).firstOrNull()?.toDomain() ?: CuteContact()
            }
            .flowOn(Dispatchers.IO)

    private fun fetchContacts(
        extraSelection: String? = null,
        extraSelectionArgs: Array<String> = emptyArray(),
    ): List<CuteContactEntity> {
        val contacts = mutableListOf<CuteContactEntity>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.STARRED
        )

        val allPhones = fetchAllPhoneNumbers()
        val accountNames = fetchAccountNames()

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            extraSelection,
            extraSelectionArgs,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->

            val idCol =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameCol =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val starCol =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)
            val photoCol =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)

                contacts.add(
                    CuteContactEntity(
                        id = id,
                        displayName = cursor.getString(nameCol) ?: "<unknown>",
                        isFavorite = cursor.getInt(starCol) == 1,
                        thumbnail = cursor.getString(photoCol),
                        accountName = accountNames[id] ?: "Device",
                        phoneNumbers = allPhones[id] ?: emptyList()
                    )
                )
            }
        }

        return contacts
    }

    private fun fetchAllPhoneNumbers(): Map<Long, List<ContactPhone>> {
        val map = mutableMapOf<Long, MutableList<ContactPhone>>()


        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
            ),
            null,
            null,
            null
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                )
            val numColumn =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
            val typeColumn =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val primColumn =
                cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
                )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val number = cursor.getString(numColumn)

                val phone = ContactPhone(
                    number = number.beautifyNumber(),
                    type = cursor.getInt(typeColumn),
                    isDefault = cursor.getInt(primColumn) != 0,
                    isBlocked = BlockedNumberContract.isBlocked(context, number)
                )

                map.getOrPut(id) { mutableListOf() }.add(phone)
            }
        }

        return map
    }

    private fun fetchContactDetails(contactId: Long): CuteContactDetails {
        val builder = CuteContactDetailsBuilder()

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.IS_PRIMARY,
                ContactsContract.Data.PHOTO_URI
            ),
            "${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->

            val mimeColumn =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Column =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Column =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data3Column =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
            val isDefaultColumn =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY)
            val photoColumn =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_URI)


            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeColumn)
                val data1 = cursor.getString(data1Column) ?: continue
                val data2 = cursor.getInt(data2Column)
                val isDefault = cursor.getInt(isDefaultColumn) != 0


                if (cursor.isFirst) {
                    val photo = cursor.getString(photoColumn)?.ifEmpty { null }
                    builder.setPhoto(photo)
                }

                when (mime) {
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ->
                        builder.addEmail(
                            ContactEmail(data1, data2, isDefault)
                        )

                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        builder.setFirstName(cursor.getString(data2Column))
                        builder.setLastName(cursor.getString(data3Column))
                    }

                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE ->
                        builder.setCompany(data1)

                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE ->
                        builder.setNote(contactNote = data1.ifEmpty { null })

                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE ->
                        builder.addEvent(ContactEvent(data1, data2))

                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE ->
                        builder.addWebsite(data1)

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE ->
                        builder.addAddress(
                            ContactAddress(data1, data2, isDefault)
                        )
                }
            }
        }

        return builder.build()
    }

    suspend fun fetchContactRawContacts(contactId: Long): List<CuteRawContact> = withContext(Dispatchers.IO) {

        val rawContacts = mutableListOf<CuteRawContact>()

        val uri = ContactsContract.RawContacts.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.ACCOUNT_NAME,
        )
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.RAW_CONTACT_IS_READ_ONLY} = ?"
        val selectionArgs = arrayOf(
            contactId.toString(),
            "0"
        )

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val rawIdColumn = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
            val accountNameColumn = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)

            while (cursor.moveToNext()) {
                val rawId = cursor.getLong(rawIdColumn)
                val accountName = cursor.getString(accountNameColumn)
                val rawContact = CuteRawContact(
                    id = rawId,
                    accountName = accountName
                )
                rawContacts.add(rawContact)
            }
        }
        return@withContext rawContacts
    }


    suspend fun fetchRawContactEdit(rawContactId: Long): RawContactEdit =
        withContext(Dispatchers.IO) {
            var contactId = 0L
            var displayName = ""
            var isFavorite = false

            var firstName: String? = null
            var middleName: String? = null
            var lastName: String? = null
            var company: String? = null
            var note: String? = null
            var photo: String? = null

            val phoneNumbers = mutableListOf<ContactPhone>()
            val emails = mutableListOf<ContactEmail>()
            val addresses = mutableListOf<ContactAddress>()
            val events = mutableListOf<ContactEvent>()
            val websites = mutableListOf<String>()

            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME_PRIMARY,
                ContactsContract.Data.STARRED,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.DATA5,
                ContactsContract.Data.IS_PRIMARY,
                ContactsContract.Data.PHOTO_URI
            )

            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                "${ContactsContract.Data.RAW_CONTACT_ID} = ?",
                arrayOf(rawContactId.toString()),
                null
            )?.use { cursor ->
                val contactIdCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                val displayNameCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
                val starredCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.STARRED)
                val mimeCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
                val data1Col = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
                val data2Col = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
                val data3Col = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
                val data5Col = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA5)
                val isPrimaryCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY)
                val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_URI)

                while (cursor.moveToNext()) {

                    if (cursor.isFirst) {
                        contactId = cursor.getLong(contactIdCol)
                        displayName = cursor.getString(displayNameCol) ?: ""
                        isFavorite = cursor.getInt(starredCol) == 1
                        photo = cursor.getString(photoCol)?.ifEmpty { null }
                    }

                    val mime = cursor.getString(mimeCol) ?: continue
                    val isDefault = cursor.getInt(isPrimaryCol) != 0

                    when (mime) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            val number = cursor.getString(data1Col) ?: continue
                            phoneNumbers.add(
                                ContactPhone(
                                    number = number,
                                    type = cursor.getInt(data2Col),
                                    isDefault = isDefault,
                                    isBlocked = BlockedNumberContract.isBlocked(context, number)
                                )
                            )
                        }
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            val email = cursor.getString(data1Col) ?: continue
                            emails.add(
                                ContactEmail(
                                    email = email,
                                    type = cursor.getInt(data2Col),
                                    isDefault = isDefault
                                )
                            )
                        }
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                            firstName = cursor.getString(data2Col)
                            middleName = cursor.getString(data5Col)
                            lastName = cursor.getString(data3Col)
                        }
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                            company = cursor.getString(data1Col)
                        }
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                            note = cursor.getString(data1Col)?.ifEmpty { null }
                        }
                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                            val date = cursor.getString(data1Col) ?: continue
                            events.add(ContactEvent(date, cursor.getInt(data2Col)))
                        }
                        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                            val website = cursor.getString(data1Col) ?: continue
                            websites.add(website)
                        }
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            val address = cursor.getString(data1Col) ?: continue
                            addresses.add(
                                ContactAddress(
                                    address = address,
                                    type = cursor.getInt(data2Col),
                                    isDefault = isDefault
                                )
                            )
                        }
                    }
                }
            }

            return@withContext RawContactEdit(
                rawContactId = rawContactId,
                contactId = contactId,
                displayName = displayName,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                company = company,
                note = note,
                photoString = photo,
                isFavorite = isFavorite,
                phoneNumbers = phoneNumbers.toList(),
                emails = emails.toList(),
                addresses = addresses.toList(),
                events = events.toList(),
                websites = websites.toList()
            )
        }



    private fun fetchAccountNames(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.ACCOUNT_NAME
            ),
            null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idCol)
                if (!map.containsKey(contactId)) {
                    map[contactId] = cursor.getString(nameCol) ?: "Device"
                }
            }
        }
        return map
    }

    suspend fun createOrEditContact(
        rawContact: RawContactEdit,
    ): Boolean = withContext(Dispatchers.IO) {

        val rawId = rawContact.rawContactId

        return@withContext try {
            val operations = arrayListOf<ContentProviderOperation>()
            val pfpByteArray = rawContact.photoString?.toUri()?.let { uriToByteArray(it) }


            if (rawId == null) {
                // create new contact

                val rawContactBuilder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValues(contentValuesOf())

                operations.add(rawContactBuilder.build())

                val backRefIndex = 0

                if (pfpByteArray != null) {
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, pfpByteArray)
                            .build()
                    )
                }

                if (!rawContact.firstName.isNullOrBlank() || !rawContact.lastName.isNullOrBlank() || !rawContact.middleName.isNullOrBlank()) {
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, rawContact.firstName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, rawContact.middleName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, rawContact.lastName)
                            .build()
                    )
                }

                rawContact.phoneNumbers.forEach { phone ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                            .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, if (phone.isDefault) 1 else 0)
                            .build()
                    )
                }

                rawContact.emails.forEach { email ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                            .withValue(ContactsContract.CommonDataKinds.Email.IS_PRIMARY, if (email.isDefault) 1 else 0)
                            .build()
                    )
                }

                rawContact.addresses.forEach { address ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.address)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, address.type)
                            .build()
                    )
                }

                rawContact.websites.forEach { website ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Website.URL, website)
                            .build()
                    )
                }

                if (!rawContact.note.isNullOrBlank()) {
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, rawContact.note)
                            .build()
                    )
                }

                rawContact.events.forEach { event ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, backRefIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, event.date)
                            .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.type)
                            .build()
                    )
                }

            } else {
                // edit existing raw contact

                val rawContactSelection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
                        .build()
                )
                if (!rawContact.firstName.isNullOrBlank() || !rawContact.lastName.isNullOrBlank() || !rawContact.middleName.isNullOrBlank()) {
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, rawContact.firstName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, rawContact.middleName)
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, rawContact.lastName)
                            .build()
                    )
                }

                if (pfpByteArray != null) {
                    operations.add(
                        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE))
                            .build()
                    )
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, pfpByteArray)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE))
                        .build()
                )
                rawContact.phoneNumbers.forEach { phone ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                            .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, if (phone.isDefault) 1 else 0)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE))
                        .build()
                )
                rawContact.emails.forEach { email ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                            .withValue(ContactsContract.CommonDataKinds.Email.IS_PRIMARY, if (email.isDefault) 1 else 0)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE))
                        .build()
                )
                rawContact.addresses.forEach { address ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.address)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, address.type)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE))
                        .build()
                )
                rawContact.websites.forEach { website ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Website.URL, website)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE))
                        .build()
                )
                rawContact.events.forEach { event ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, event.date)
                            .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.type)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(rawContactSelection, arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE))
                        .build()
                )
                if (!rawContact.note.isNullOrBlank()) {
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, rawContact.note)
                            .build()
                    )
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving contact: ${e.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    private fun uriToByteArray(uri: Uri?): ByteArray? {

        if (uri == null || uri == Uri.EMPTY) return null

        context.contentResolver.openInputStream(uri)?.use {
            return it.readBytes()
        }

        return null
    }

    suspend fun deleteContacts(contactIds: List<Long>) = withContext(Dispatchers.IO) {

        try {
            val ops = ArrayList<ContentProviderOperation>()
            contactIds.fastForEach { id ->
                ops.add(
                    ContentProviderOperation
                        .newDelete(ContactsContract.Contacts.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Contacts._ID} = ?",
                            arrayOf(id.toString())
                        )
                        .build()
                )
            }
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    suspend fun toggleFavorite(contacts: List<CuteContact>) = withContext(Dispatchers.IO) {

        val ops = ArrayList<ContentProviderOperation>()

        contacts.fastForEach { contact ->
            val id = contact.id
            val isFavorite = contact.isFavorite
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id.toString())
            ops.add(
                ContentProviderOperation.newUpdate(uri)
                    .withValue(ContactsContract.Contacts.STARRED, if (isFavorite) 0 else 1)
                    .build()
            )
        }
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    suspend fun blockContact(
        phones: List<String>,
        emails: List<String> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {

        val ops = ArrayList<ContentProviderOperation>()

        (phones + emails).fastForEach { number ->
            ops.add(
                ContentProviderOperation.newInsert(BlockedNumbers.CONTENT_URI)
                    .withValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                    .build()
            )
        }
        return@withContext try {
            context.contentResolver.applyBatch(BlockedNumberContract.AUTHORITY, ops)
            true
        } catch (_: Exception) {
            false
        }

    }
}