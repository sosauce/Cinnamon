package com.sosauce.cinnamon.data.receivers

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.klinker.android.send_message.MmsReceivedReceiver

class MmsReceiver : MmsReceivedReceiver() {
    override fun onMessageReceived(context: Context, uri: Uri?) {}

    override fun onError(context: Context, error: String?) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }
}
