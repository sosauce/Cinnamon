package com.sosauce.cuteconnect.data.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.contentValuesOf
import com.klinker.android.send_message.MmsSentReceiver.EXTRA_CONTENT_URI
import com.sosauce.cuteconnect.data.managers.MessageNotificationManager
import com.sosauce.cuteconnect.domain.repository.CommonRepository
import androidx.core.net.toUri

class MmsReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val uri = intent.getStringExtra(EXTRA_CONTENT_URI)?.toUri() ?: return

        val messageBox = if (resultCode == Activity.RESULT_OK) {
            Telephony.Mms.MESSAGE_BOX_SENT
        } else { Telephony.Mms.MESSAGE_BOX_FAILED }

        val values = contentValuesOf(
            Telephony.Mms.MESSAGE_BOX to messageBox
        )

        context.contentResolver.update(uri, values, null, null)

    }
}