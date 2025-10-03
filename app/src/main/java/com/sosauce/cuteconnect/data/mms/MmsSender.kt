package com.sosauce.cuteconnect.data.mms

import android.content.Context
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import com.sosauce.cuteconnect.domain.model.CuteAttachment

class MmsSender(private val context: Context) {

    fun sendMms(
        addresses: List<String>,
        message: String,
        attachment: CuteAttachment?,
        threadId: Long
    ) {
        val settings = Settings().apply {
            useSystemSending = true
            deliveryReports = true
            sendLongAsMms = true
            group = true
        }
        val transaction = Transaction(context, settings)
        val message = Message(message, addresses.toTypedArray())

        if (attachment == null) {
            // Group chat condition most likely


        }


        transaction.sendNewMessage(message, threadId)
    }

}