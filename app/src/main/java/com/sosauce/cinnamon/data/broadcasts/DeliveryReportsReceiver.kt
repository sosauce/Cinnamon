package com.sosauce.cinnamon.data.broadcasts

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.contentValuesOf

class DeliveryReportsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val uri = intent?.data ?: return


        val status = if (resultCode == Activity.RESULT_OK) {
            Telephony.Sms.STATUS_COMPLETE
        } else Telephony.Sms.STATUS_FAILED

        val values = contentValuesOf(
            Telephony.Sms.STATUS to status
        )

        context.contentResolver.update(uri, values, null, null)
    }

}