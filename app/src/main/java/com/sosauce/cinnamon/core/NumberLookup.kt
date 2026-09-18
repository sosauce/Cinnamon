package com.sosauce.cinnamon.core

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A class that allows to lookup for various data for a given phone number
 */
class NumberLookup(
    private val context: Context
) {

    /**
     * @param fullQuality If false, will provide a thumbnail
     * @return The image [android.net.Uri] as a [String] or null if not found.
     */
    suspend fun fetchPhoto(
        number: String,
        fullQuality: Boolean
    ): String? = withContext(Dispatchers.IO) {

        if (number.isEmpty()) return@withContext null

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))

        val photoQuality = if (fullQuality) ContactsContract.PhoneLookup.PHOTO_URI else ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI

        context.contentResolver.query(
            uri,
            arrayOf(photoQuality),
            null,
            null,
            null
        )?.use { cursor ->

            val photoColumn = cursor.getColumnIndexOrThrow(photoQuality)

            if (cursor.moveToFirst()) {
                return@withContext cursor.getString(photoColumn)
            }

        }
        return@withContext null
    }

    /**
     * @return The display name of the contact associated with [number] if it exists.
     */
    fun fetchContactDisplayName(
        number: String
    ): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))

        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)

            if (cursor.moveToFirst()) {
                return cursor.getString(nameColumn)
            }

        }
        return null
    }

}