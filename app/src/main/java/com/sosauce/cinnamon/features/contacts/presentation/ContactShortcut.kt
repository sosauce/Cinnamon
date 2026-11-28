package com.sosauce.cinnamon.features.contacts.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ThumbnailUtils
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.MainActivity
import com.sosauce.cinnamon.core.utils.CuteIntents
import com.sosauce.cinnamon.features.contacts.domain.CuteContact

private const val ICON_SIZE = 432

suspend fun pinContactShortcut(
    context: Context,
    contact: CuteContact,
    photo: Uri?,
    @ColorInt background: Int,
    @ColorInt foreground: Int
): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false

    val icon = photo?.let { loadSquarePhoto(context, it) }
        ?: placeholderIcon(context, contact.displayName.firstOrNull(), background, foreground)

    val shortcut = ShortcutInfoCompat.Builder(context, "contact_${contact.id}")
        .setShortLabel(contact.displayName.ifBlank { context.getString(R.string.unknown) })
        .setIcon(IconCompat.createWithAdaptiveBitmap(icon))
        .setIntent(
            Intent(context, MainActivity::class.java)
                .setAction(CuteIntents.SHORTCUT_CONTACT_DETAILS)
                .putExtra("contactId", contact.id)
        )
        .build()

    return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}

private suspend fun loadSquarePhoto(context: Context, photo: Uri): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(photo)
        .size(ICON_SIZE)
        .allowHardware(false)
        .build()
    val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return null
    return ThumbnailUtils.extractThumbnail(bitmap, ICON_SIZE, ICON_SIZE)
}

private fun placeholderIcon(
    context: Context,
    firstLetter: Char?,
    @ColorInt background: Int,
    @ColorInt foreground: Int
): Bitmap {
    val bitmap = createBitmap(ICON_SIZE, ICON_SIZE)
    val canvas = Canvas(bitmap)
    canvas.drawColor(background)

    if (firstLetter?.isLetter() == true) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foreground
            textSize = ICON_SIZE / 3f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val baseline = ICON_SIZE / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(firstLetter.uppercase(), ICON_SIZE / 2f, baseline, paint)
    } else {
        val inset = ICON_SIZE / 3
        ContextCompat.getDrawable(context, R.drawable.person_filled)?.apply {
            setTint(foreground)
            setBounds(inset, inset, ICON_SIZE - inset, ICON_SIZE - inset)
            draw(canvas)
        }
    }

    return bitmap
}
