package com.sosauce.cinnamon.data.providers

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sosauce.cinnamon.R
import java.io.File

class ComposeFileProvider : FileProvider(R.xml.file_paths) {
    companion object {
        fun getImageUri(context: Context): Uri {
            val directory = File(context.cacheDir, "images")
            directory.mkdirs()
            val file = File.createTempFile(
                "mms_image_",
                ".jpg",
                directory
            )
            val authority = context.packageName + ".fileprovider"
            return getUriForFile(
                context,
                authority,
                file,
            )
        }
    }
}