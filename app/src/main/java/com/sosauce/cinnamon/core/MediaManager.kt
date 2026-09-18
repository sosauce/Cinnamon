package com.sosauce.cinnamon.core

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class MediaManager(
    private val context: Context
) {

    /**
     * @return Whether the image was successfully saved or not
     */
    suspend fun saveImageToDevice(image: Uri): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val contentValues = contentValuesOf(
            MediaStore.Images.Media.DISPLAY_NAME to "mms_${System.currentTimeMillis()}.jpg",
            MediaStore.Images.Media.MIME_TYPE to "image/jpeg",
            MediaStore.Images.Media.RELATIVE_PATH to Environment.DIRECTORY_PICTURES,
            MediaStore.Images.Media.IS_PENDING to 1,
        )

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return@withContext false

        try {
            resolver.openInputStream(image)?.use { input ->
                resolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                } ?: throw IOException("Failed to open output stream")
            } ?: throw IOException("Failed to open input stream")

            resolver.update(
                uri,
                contentValuesOf(
                    MediaStore.Images.Media.IS_PENDING to 0
                ),
                null,
                null
            )

            true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

}