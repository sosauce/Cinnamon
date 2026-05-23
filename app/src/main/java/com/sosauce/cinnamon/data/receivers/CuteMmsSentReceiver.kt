package com.sosauce.cinnamon.data.receivers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import com.klinker.android.send_message.MmsSentReceiver
import java.io.File

// TODO: I just copied fossify for this one, don't forget to rewrite/cleanup for my needs

class CuteMmsSentReceiver : MmsSentReceiver() {


    override fun onMessageStatusUpdated(context: Context?, intent: Intent?, resultCode: Int) {
        super.onMessageStatusUpdated(context, intent, resultCode)

        println("I forgot: ${intent?.data}, $resultCode")

        updateMmsStatus(context!!, intent!!, resultCode)
        SmsManager.MMS_CONFIG_MMS_ENABLED
    }

    private fun updateMmsStatus(context: Context, intent: Intent, resultCode: Int) {
        val uri = intent.getStringExtra(EXTRA_CONTENT_URI)?.toUri() ?: return
        val messageBox = if (resultCode == Activity.RESULT_OK) {
            Telephony.Mms.MESSAGE_BOX_SENT
        } else Telephony.Mms.MESSAGE_BOX_FAILED

        println("I forgot: MESSAGE BOX $messageBox")

        val values = contentValuesOf(Telephony.Mms.MESSAGE_BOX to messageBox)

        context.contentResolver.update(uri, values, null, null)

        intent.getStringExtra(EXTRA_FILE_PATH)?.let {
            File(it).delete()
        }
    }


    companion object {
        private const val EXTRA_CONTENT_URI = "content_uri"
        private const val EXTRA_FILE_PATH = "file_path"
    }
}