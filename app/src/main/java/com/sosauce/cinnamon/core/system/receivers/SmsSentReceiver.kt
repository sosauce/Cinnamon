package com.sosauce.cinnamon.core.system.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageUri = intent.getStringExtra(MESSAGE_URI)?.toUri() ?: return
        val values = contentValuesOf(
            Telephony.Sms.TYPE to if (resultCode == Activity.RESULT_OK) {
                Telephony.Sms.MESSAGE_TYPE_SENT
            } else {
                Telephony.Sms.MESSAGE_TYPE_FAILED
            }
        )
        val selection = if (resultCode == Activity.RESULT_OK) {
            "${Telephony.Sms.TYPE} != ?"
        } else {
            null
        }
        val selectionArgs = if (selection != null) {
            arrayOf(Telephony.Sms.MESSAGE_TYPE_FAILED.toString())
        } else {
            null
        }
        context.contentResolver.update(messageUri, values, selection, selectionArgs)
    }

    companion object {
        const val MESSAGE_URI = "message_uri"
    }
}
