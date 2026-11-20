package com.sosauce.cinnamon.core

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.toBitmap
import coil3.transform.CircleCropTransformation
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

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        val photoQuality =
            if (fullQuality) ContactsContract.PhoneLookup.PHOTO_URI else ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI

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
     * Round thumbnail of the contact associated with [number], meant for a notification's person
     * @return null if there is no photo
     */
    suspend fun fetchPersonIcon(number: String): IconCompat? =
        fetchPersonIcon(fetchPhoto(number, false)?.toUri())

    /**
     * Round icon of [photo], meant for a notification's person
     */
    suspend fun fetchPersonIcon(photo: Uri?): IconCompat? {
        if (photo == null) return null

        val request = ImageRequest.Builder(context)
            .data(photo)
            .transformations(CircleCropTransformation())
            .build()
        val bitmap = context.imageLoader.execute(request).image?.toBitmap()

        return bitmap?.let { IconCompat.createWithBitmap(it) }
    }

    /**
     * Photo of the contact on a call: the one Telecom resolved (API 34+), else our own lookup for [number].
     * @param fullQuality If false, our own lookup provides a thumbnail (Telecom's photo is always full size)
     * @return null when there's no photo
     */
    suspend fun fetchCallerPhoto(
        details: Call.Details?,
        number: String,
        fullQuality: Boolean
    ): Uri? = details?.telecomContactPhotoUri ?: fetchPhoto(number, fullQuality)?.toUri()

    /**
     * Combined lookup for [fetchContactDisplayName] + [fetchPhoto] in a single
     * `PhoneLookup` query. The key must always be the raw number string.
     *
     * @param fullQuality If false, will provide a thumbnail
     * @return Pair(displayName, photoUriString), each null when not found.
     */
    suspend fun fetchContactInfo(
        number: String,
        fullQuality: Boolean
    ): Pair<String?, String?> = withContext(Dispatchers.IO) {
        if (number.isEmpty()) return@withContext null to null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        val photoQuality =
            if (fullQuality) ContactsContract.PhoneLookup.PHOTO_URI else ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI

        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                photoQuality
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val photoColumn = cursor.getColumnIndexOrThrow(photoQuality)

            if (cursor.moveToFirst()) {
                return@withContext cursor.getString(nameColumn) to cursor.getString(photoColumn)
            }
        }
        return@withContext null to null
    }

    /**
     * Name of the contact on a call: the one Telecom resolved (API 30+), else our own lookup for [number].
     * @return null when the caller isn't a contact
     */
    fun fetchCallerName(
        details: Call.Details?,
        number: String
    ): String? = details?.telecomContactName ?: fetchContactDisplayName(number)


    /**
     * @return The display name of the contact associated with [number] if it exists.
     */
    fun fetchContactDisplayName(
        number: String
    ): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

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

/**
 * Always null below API 30.
 */
val Call.Details.telecomContactName: String?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        contactDisplayName?.takeIf { it.isNotBlank() }
    } else {
        null
    }

/**
 * Contact photo Telecom resolved for this call, can show up later through onDetailsChanged. Always null below API 34.
 */
val Call.Details.telecomContactPhotoUri: Uri?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) contactPhotoUri else null
