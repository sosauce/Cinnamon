package com.sosauce.cinnamon.features.contacts.presentation.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import com.sosauce.cinnamon.R

/**
 * Opens the system ringtone picker, preselecting the given ringtone (null means the default one).
 *
 * Returns null when the picker was cancelled, otherwise the [Picked] ringtone
 */
class PickRingtone : ActivityResultContract<Uri?, PickRingtone.Picked?>() {

    data class Picked(val uri: Uri?)

    override fun createIntent(context: Context, input: Uri?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.ringtone))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_RINGTONE_URI)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                input ?: Settings.System.DEFAULT_RINGTONE_URI
            )
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Picked? {
        if (resultCode != Activity.RESULT_OK) return null

        return Picked(
            intent?.let {
                IntentCompat.getParcelableExtra(
                    it,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java
                )
            }
        )
    }
}
