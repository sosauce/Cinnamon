package com.sosauce.cinnamon.core.system.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.content.contentValuesOf

class DeliveryReportsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val uri = intent?.data ?: return
        val pdu = intent.getByteArrayExtra("pdu") ?: return
        val format = intent.getStringExtra("format")
        val message = if (format != null) {
            SmsMessage.createFromPdu(pdu, format)
        } else {
            @Suppress("DEPRECATION")
            SmsMessage.createFromPdu(pdu)
        } ?: return

        val status = if (message.status in 0..31) {
            Telephony.Sms.STATUS_COMPLETE
        } else Telephony.Sms.STATUS_FAILED

        val values = contentValuesOf(
            Telephony.Sms.STATUS to status
        )

        context.contentResolver.update(uri, values, null, null)
    }

}
