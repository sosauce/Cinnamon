package com.sosauce.cinnamon.data.fetchers

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.sosauce.cinnamon.utils.PermissionUtils
import okio.FileSystem
import okio.buffer
import okio.source

class RecipientPhotoFetcher(
    private val context: Context,
    private val recipient: RecipientPhone
) : Fetcher {
    override suspend fun fetch(): FetchResult? {

        val phoneNumber = recipient.number
        if (phoneNumber.isEmpty() || !PermissionUtils.hasContactsReadPermission(context)) {
            return null
        }


        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val photoColumnName = if (recipient.quality == PhotoQuality.FULL_QUALITY) {
            ContactsContract.PhoneLookup.PHOTO_URI
        } else {
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        }
        var pfpContactUri: Uri? = null

        context.contentResolver.query(
            lookupUri,
            arrayOf(photoColumnName),
            null,
            null
        )?.use { cursor ->
            val photoColumn = cursor.getColumnIndexOrThrow(photoColumnName)
            if (cursor.moveToFirst()) {
                pfpContactUri = cursor.getString(photoColumn)?.toUri()
            }
        }

        val finalUri = pfpContactUri ?: return null
        val inputStream = context.contentResolver.openInputStream(finalUri) ?: return null

        return SourceFetchResult(
            source = ImageSource(
                source = inputStream.source().buffer(),
                fileSystem = FileSystem.SYSTEM
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<RecipientPhone> {
        override fun create(
            data: RecipientPhone,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return RecipientPhotoFetcher(options.context, data)
        }
    }

}

enum class PhotoQuality { THUMBNAIL, FULL_QUALITY }

data class RecipientPhone(
    val number: String,
    val quality: PhotoQuality = PhotoQuality.THUMBNAIL
)

class RecipientPhoneKeyer : Keyer<RecipientPhone> {
    override fun key(
        data: RecipientPhone,
        options: Options
    ): String = "${data.number}_${data.quality.name}"
}



