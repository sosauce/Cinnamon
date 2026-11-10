@file:OptIn(DelicateCoroutinesApi::class)

package com.sosauce.cinnamon.core.system.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sosauce.cinnamon.core.telephony.message.CuteTelephonyManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class QuickReplyService : Service(), KoinComponent {

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val telephonyManager by inject<CuteTelephonyManager>()

        val addresses = intent?.data?.schemeSpecificPart
            ?.split(Regex("[;,]"))
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)

        if (addresses.isNotEmpty() && !text.isNullOrEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    telephonyManager.sendMessage(
                        addresses = addresses,
                        message = text,
                        attachments = emptyList()
                    )
                } finally {
                    stopSelf(startId)
                }
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

}
