@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.domain.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.widget.Toast
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.model.CuteContactDetails
import com.sosauce.cinnamon.domain.model.CuteContactDetailsBuilder
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val context: Context
) {


    fun fetchLatestContacts(
        extraSelection: String? = null,
        extraSelectionArgs: Array<String> = emptyArray()
    ) = context.contentResolver.observe(Contacts.CONTENT_URI).mapLatest {
        fetchContacts(extraSelection, extraSelectionArgs)
    }.flowOn(Dispatchers.IO)

    fun fetchLatestContactsDetails(contactId: Long) =
        context.contentResolver.observe(ContactsContract.Data.CONTENT_URI).mapLatest {
            fetchContactDetails(contactId)
        }.flowOn(Dispatchers.IO)

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

    private fun fetchContacts(
        extraSelection: String?,
        extraSelectionArgs: Array<String>,
    ): List<CuteContact> {
        val contacts = mutableListOf<CuteContact>()

        val allPhones = fetchAllPhoneNumbers()

        val accountNames = fetchAccountNames()

        context.contentResolver.query(
            Contacts.CONTENT_URI,
            arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.STARRED,
                Contacts.PHOTO_THUMBNAIL_URI
            ),
            extraSelection,
            extraSelectionArgs,
            "${Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Contacts._ID)
            val nameCol = cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY)
            val starCol = cursor.getColumnIndexOrThrow(Contacts.STARRED)
            val photoCol = cursor.getColumnIndexOrThrow(Contacts.PHOTO_THUMBNAIL_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val accountName = accountNames[id] ?: "Device"

                contacts.add(
                    CuteContact(
                        id = id,
                        displayName = cursor.getString(nameCol) ?: "",
                        photo = cursor.getString(photoCol)?.toUri() ?: Uri.EMPTY,
                        isFavorite = cursor.getInt(starCol) == 1,
                        details = CuteContactDetails(phoneNumbers = allPhones[id] ?: emptyList()),
                        accountName = accountName
                    )
                )
            }
        }
        return contacts
    }

    private fun fetchAllPhoneNumbers(): Map<Long, List<CuteContact.Phone>> {
        val map = mutableMapOf<Long, MutableList<CuteContact.Phone>>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.IS_PRIMARY
            ),
            null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val numCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val typeCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val primCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val phone = CuteContact.Phone(
                    number = cursor.getString(numCol),
                    type = cursor.getInt(typeCol),
                    isDefault = cursor.getInt(primCol) != 0
                )
                map.getOrPut(id) { mutableListOf() }.add(phone)
            }
        }
        return map
    }


    private fun fetchContactDetails(contactId: Long): CuteContact {
        val builder = CuteContactDetailsBuilder()

        val uri = ContactsContract.Data.CONTENT_URI
        val selection = "${ContactsContract.Data.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        var displayName = ""
        var photoUri = Uri.EMPTY
        var starred = false

        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.Data.DISPLAY_NAME_PRIMARY,
                ContactsContract.Data.PHOTO_URI,
                ContactsContract.Data.STARRED,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.IS_PRIMARY
            ),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
            val photoColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_URI)
            val starredColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.STARRED)
            val mimeColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data3Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
            val isDefaultColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY)

            while (cursor.moveToNext()) {

                if (cursor.isFirst) {
                    displayName = cursor.getString(displayNameColumn) ?: ""
                    photoUri = cursor.getString(photoColumn)?.toUri() ?: Uri.EMPTY
                    starred = cursor.getInt(starredColumn) != 0
                }

                val mime = cursor.getString(mimeColumn)
                val data1 = cursor.getString(data1Column) ?: continue
                val data2 = cursor.getInt(data2Column)
                val isDefault = cursor.getInt(isDefaultColumn) != 0

                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ->
                        builder.addPhoneNumber(CuteContact.Phone(data1, data2, isDefault))

                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ->
                        builder.addEmail(CuteContact.Email(data1, data2, isDefault))

                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        builder.firstName = cursor.getString(data2Column) ?: ""
                        builder.lastName = cursor.getString(data3Column) ?: ""
                    }

                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> builder.company =
                        data1

                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> builder.note = data1
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> builder.addEvent(
                        CuteContact.Event(data1, data2)
                    )

                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> builder.addWebsite(
                        CuteContact.Website(data1)
                    )

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> builder.addAddress(
                        CuteContact.Address(data1, data2, isDefault)
                    )
                }
            }
        }
        return CuteContact(
            id = contactId,
            displayName = displayName,
            isFavorite = starred,
            photo = photoUri,
            details = builder.build()
        )
    }

    /**
     * Gets the RAW_CONTACT_ID that is required to edit a contact, 0 if contact doesn't exist (so we're creating one)
     */
    private suspend fun getContactRawId(contactId: Long): Long = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.RAW_CONTACT_IS_READ_ONLY} = ?", // https://developer.android.com/reference/android/provider/ContactsContract.RawContactsColumns.html?utm_source=android-studio-app&utm_medium=app#RAW_CONTACT_IS_READ_ONLY
            arrayOf(contactId.toString(), "0"),
            null
        )?.use { cursor ->

            val rawIdColumn = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)

            if (cursor.moveToFirst()) {
                return@withContext cursor.getLong(rawIdColumn)
            }
        }

        return@withContext 0
    }

    suspend fun createOrEditContact(
        contact: CuteContact
    ): Boolean = withContext(Dispatchers.IO) {

        val rawId = getContactRawId(contact.id)


        return@withContext try {
            val operations = arrayListOf<ContentProviderOperation>()

            val pfpByteArray = uriToByteArray(contact.photo)

            if (rawId == 0L) {
                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contact.accountType)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.accountName)
                        .withYieldAllowed(true)
                        .build()
                )


                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, pfpByteArray)
                        .withYieldAllowed(true)
                        .build()
                )
                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                            contact.details.firstName
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                            contact.details.middleName
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                            contact.details.lastName
                        )
                        .withYieldAllowed(true) // from what I understand, this allows the content resolver to not take too long/freeze thread for each operation
                        .build()
                )

                contact.details.phoneNumbers.fastForEach { phone ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                                if (phone.isDefault) 1 else 0
                            )
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                contact.details.emails.fastForEach { email ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
                                if (email.isDefault) 1 else 0
                            )
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                contact.details.addresses.fastForEach { address ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                                address.address
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                                address.type
                            )
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                contact.details.websites.fastForEach { website ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Website.URL,
                                website.website
                            )
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.details.note)
                        .withYieldAllowed(true)
                        .build()
                )

                contact.details.events.fastForEach { event ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Event.START_DATE,
                                event.date
                            )
                            .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.type)
                            .withYieldAllowed(true)
                            .build()
                    )
                }

            } else {


                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                            contact.details.firstName
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                            contact.details.middleName
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                            contact.details.lastName
                        )
                        .withYieldAllowed(true) // from what I understand, this allows the content resolver to not take too long/freeze thread for each operation
                        .build()

                )


                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                        )
                        .build()
                )

                contact.details.phoneNumbers.fastForEach { phone ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                                if (phone.isDefault) 1 else 0
                            )
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                        )
                        .build()
                )


                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                            )
                        )
                        .withYieldAllowed(true)
                        .build()
                )

                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, pfpByteArray)
                        .withYieldAllowed(true)
                        .build()
                )

                contact.details.emails.fastForEach { email ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
                                if (email.isDefault) 1 else 0
                            )
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                            )
                        )
                        .build()
                )

                contact.details.addresses.fastForEach { address ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                                address.address
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                                address.type
                            )
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE
                            )
                        )
                        .build()
                )

                contact.details.websites.fastForEach { website ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Website.URL,
                                website.website
                            )
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
                            )
                        )
                        .build()
                )

                contact.details.events.fastForEach { event ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Event.START_DATE,
                                event.date
                            )
                            .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.type)
                            .build()
                    )
                }
                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawId.toString(),
                                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                            )
                        )
                        .build()
                )

                if (!contact.details.note.isNullOrBlank()) {
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Note.NOTE,
                                contact.details.note
                            )
                            .build()
                    )
                }


            }
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error while saving contact", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    private fun uriToByteArray(uri: Uri): ByteArray? {

        if (uri == Uri.EMPTY) return null

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
                        .newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.RawContacts._ID} = ?",
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
            val uri = Uri.withAppendedPath(Contacts.CONTENT_URI, id.toString())
            ops.add(
                ContentProviderOperation.newUpdate(uri)
                    .withValue(Contacts.STARRED, if (isFavorite) 0 else 1)
                    .build()
            )
        }
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }


//    fun fetchAccounts(): List<Account> = AccountManager.get(context).accounts.toList()
}