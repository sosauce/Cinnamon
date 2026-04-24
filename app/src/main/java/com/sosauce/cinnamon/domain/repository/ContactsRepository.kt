@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.domain.repository

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.widget.Toast
import androidx.compose.ui.util.fastForEach
import androidx.core.content.contentValuesOf
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.domain.model.CuteContactDetails
import com.sosauce.cinnamon.domain.model.CuteContactDetailsBuilder
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlin.collections.plus

class ContactsRepository(
    private val context: Context
) {


    fun fetchLatestContacts(
        extraSelection: String? = null,
        extraSelectionArgs: Array<String> = emptyArray()
    ) = context.contentResolver.observe(ContactsContract.Data.CONTENT_URI).mapLatest {
        fetchContacts(extraSelection, extraSelectionArgs)
    }.flowOn(Dispatchers.IO)

    private fun fetchContacts(
        extraSelection: String?,
        extraSelectionArgs: Array<String>
    ): List<CuteContact> {

        val contacts = mutableListOf<CuteContact>()

        val uri = Contacts.CONTENT_URI

        val projection = arrayOf(
            Contacts._ID,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.STARRED
        )

        val allDetails = fetchAllContactsDetails()

        context.contentResolver.query(
            uri,
            projection,
            extraSelection,
            extraSelectionArgs,
            "${Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Contacts._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY)
            val starredColumn = cursor.getColumnIndexOrThrow(Contacts.STARRED)


            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn) ?: ""
                val isFavorite = cursor.getInt(starredColumn) == 1

                val contactDetails = allDetails[id] ?: CuteContactDetails()

                contacts.add(
                    CuteContact(
                        id = id,
                        displayName = displayName,
                        photo = getContactPhoto(id),
                        isFavorite = isFavorite,
                        details = contactDetails
                    )
                )

            }
        }

        return contacts
    }


    private fun fetchAllContactsDetails(): Map<Long, CuteContactDetails> {

        val allDetails = mutableMapOf<Long, CuteContactDetailsBuilder>()

        val uri = ContactsContract.Data.CONTENT_URI

        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.IS_PRIMARY
        )


        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val contactIdColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val isPrimaryColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY)
            val data1Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data3Column = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(contactIdColumn)
                val mimeType = cursor.getString(mimeColumn)
                val isPrimary = cursor.getInt(isPrimaryColumn) != 0
                val data1 = cursor.getString(data1Column)
                val data2 = cursor.getInt(data2Column)
                val data3 = cursor.getString(data3Column)

                if (data1.isNullOrBlank()) { continue }

                val builder = allDetails.getOrPut(contactId) { CuteContactDetailsBuilder() }
                when (mimeType) {

                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ->
                        builder.addPhoneNumber(
                            CuteContact.Phone(data1, data2, isPrimary)
                        )

                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE ->
                        builder.addEmail(
                            CuteContact.Email(data1, data2, isPrimary)
                        )

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE ->
                        builder.addAddress(
                            CuteContact.Address(data1, data2, isPrimary)
                        )

                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE ->
                        builder.addWebsite(
                            CuteContact.Website(data1)
                        )

                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE ->
                        builder.addEvent(
                            CuteContact.Event(data1, data2)
                        )

                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE ->
                        builder.note = data1

                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE ->
                        builder.company = data1

                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        val firstName = cursor.getString(data2Column)
                        builder.firstName = firstName ?: ""
                        builder.lastName = data3 ?: ""
                    }

                    else -> Unit
                }

                allDetails[contactId] = builder


            }
        }
        return allDetails.mapValues { it.value.build() }
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

    private fun getContactPhoto(contactId: Long): Uri {
        val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
        val photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.DISPLAY_PHOTO)
        return photoUri
    }

    suspend fun createOrEditContact(
        contact: CuteContact,
        showProgressToasts: Boolean = false // Only show them when inserting from vCard
    ) = withContext(Dispatchers.IO) {

        val rawId = getContactRawId(contact.id)


        try {

            if (showProgressToasts) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Inserting contact", Toast.LENGTH_SHORT).show()
                }
            }
            val operations = arrayListOf<ContentProviderOperation>()

            if (rawId == 0L) {
                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .withYieldAllowed(true)
                        .build()
                )


                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.details.firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.details.middleName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.details.lastName)
                        .withYieldAllowed(true) // from what I understand, this allows the content resolver to not take too long/freeze thread for each operation
                        .build()
                )

                contact.details.phoneNumbers.fastForEach { phone ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                            .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, if (phone.isDefault) 1 else 0)
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                contact.details.emails.fastForEach { email ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                            .withValue(ContactsContract.CommonDataKinds.Email.IS_PRIMARY, if (email.isDefault) 1 else 0)
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                contact.details.addresses.fastForEach { address ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.address)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, address.type)
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                contact.details.websites.fastForEach { website ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Website.URL, website.website)
                            .withYieldAllowed(true)
                            .build()
                    )
                }

                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.details.note)
                        .withYieldAllowed(true)
                        .build()
                )

                contact.details.events.fastForEach { event ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, event.date)
                            .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.type)
                            .withYieldAllowed(true)
                            .build()
                    )
                }

            } else {


                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?", arrayOf(rawId.toString(),ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.details.firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.details.middleName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.details.lastName)
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
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
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

                contact.details.emails.fastForEach { email ->
                    operations.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
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
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                            .withValue(
                                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                                address.address
                            )
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, address.type)
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
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Website.URL, website.website)
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
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, event.date)
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
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.details.note)
                            .build()
                    )
                }


            }
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)

            if (showProgressToasts) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Contact inserted!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error while saving contact", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun deleteContacts(contactIds: List<Long>) = withContext(Dispatchers.IO) {

        val ops = ArrayList<ContentProviderOperation>()

        contactIds.fastForEach { id ->
            ops.add(
                ContentProviderOperation
                    .newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(id.toString()))
                    .build()
            )
        }
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
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


    fun fetchAccounts(): List<Account> = AccountManager.get(context).accounts.toList()
}