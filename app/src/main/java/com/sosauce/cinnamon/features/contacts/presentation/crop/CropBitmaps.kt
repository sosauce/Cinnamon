package com.sosauce.cinnamon.features.contacts.presentation.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val PREVIEW_MAX_SIZE = 2048
private const val EXPORT_MAX_SIZE = 4096

enum class CropMode(val aspectRatio: Float) {
    ProfilePicture(1f),
    Poster(9f / 16f)
}

enum class CropOp { RotateLeft, RotateRight, FlipHorizontal, FlipVertical }

class CropSource(
    val preview: Bitmap,
    val exifOps: List<CropOp>,
    val width: Int,
    val height: Int
)

fun List<CropOp>.toMatrix() = Matrix().apply {
    forEach { op ->
        when (op) {
            CropOp.RotateLeft -> postRotate(-90f, 0.5f, 0.5f)
            CropOp.RotateRight -> postRotate(90f, 0.5f, 0.5f)
            CropOp.FlipHorizontal -> postScale(-1f, 1f, 0.5f, 0.5f)
            CropOp.FlipVertical -> postScale(1f, -1f, 0.5f, 0.5f)
        }
    }
}

fun CropSource.render(ops: List<CropOp>): Bitmap =
    Bitmap.createBitmap(preview, 0, 0, preview.width, preview.height, (exifOps + ops).toMatrix(), true)

suspend fun loadCropSource(context: Context, uri: Uri): CropSource? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), PREVIEW_MAX_SIZE)
        }
        val preview = resolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@runCatching null

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        CropSource(preview, exifOps(orientation), bounds.outWidth, bounds.outHeight)
    }.getOrNull()
}

suspend fun exportCrop(
    context: Context,
    uri: Uri,
    source: CropSource,
    ops: List<CropOp>,
    normalizedCrop: RectF
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val matrix = (source.exifOps + ops).toMatrix()
        val sourceCrop = RectF(normalizedCrop).also { Matrix().apply { matrix.invert(this) }.mapRect(it) }
        val region = Rect(
            (sourceCrop.left * source.width).roundToInt(),
            (sourceCrop.top * source.height).roundToInt(),
            (sourceCrop.right * source.width).roundToInt(),
            (sourceCrop.bottom * source.height).roundToInt()
        )
        if (!region.intersect(0, 0, source.width, source.height)) return@runCatching null

        val decoded = decodeRegion(context, uri, source, region) ?: return@runCatching null
        val cropped = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val file = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        file.toUri()
    }.getOrNull()
}

@Suppress("DEPRECATION")
private fun decodeRegion(context: Context, uri: Uri, source: CropSource, region: Rect): Bitmap? {
    val resolver = context.contentResolver
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(maxOf(region.width(), region.height()), EXPORT_MAX_SIZE)
    }

    val fromRegion = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            BitmapRegionDecoder.newInstance(stream, false)?.let { decoder ->
                decoder.decodeRegion(region, options).also { decoder.recycle() }
            }
        }
    }.getOrNull()
    if (fromRegion != null) return fromRegion

    val fullOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(maxOf(source.width, source.height), EXPORT_MAX_SIZE)
    }
    val full = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, fullOptions) } ?: return null
    val ratio = full.width.toFloat() / source.width
    val left = (region.left * ratio).roundToInt().coerceIn(0, full.width - 1)
    val top = (region.top * ratio).roundToInt().coerceIn(0, full.height - 1)
    return Bitmap.createBitmap(
        full,
        left,
        top,
        (region.width() * ratio).roundToInt().coerceIn(1, full.width - left),
        (region.height() * ratio).roundToInt().coerceIn(1, full.height - top)
    )
}

private fun sampleSizeFor(size: Int, max: Int): Int {
    var sampleSize = 1
    while (size / sampleSize > max) sampleSize *= 2
    return sampleSize
}

private fun exifOps(orientation: Int) = when (orientation) {
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> listOf(CropOp.FlipHorizontal)
    ExifInterface.ORIENTATION_ROTATE_180 -> listOf(CropOp.RotateRight, CropOp.RotateRight)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> listOf(CropOp.FlipVertical)
    ExifInterface.ORIENTATION_TRANSPOSE -> listOf(CropOp.RotateRight, CropOp.FlipHorizontal)
    ExifInterface.ORIENTATION_ROTATE_90 -> listOf(CropOp.RotateRight)
    ExifInterface.ORIENTATION_TRANSVERSE -> listOf(CropOp.RotateLeft, CropOp.FlipHorizontal)
    ExifInterface.ORIENTATION_ROTATE_270 -> listOf(CropOp.RotateLeft)
    else -> emptyList()
}
