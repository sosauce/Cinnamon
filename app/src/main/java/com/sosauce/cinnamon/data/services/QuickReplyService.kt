@file:OptIn(DelicateCoroutinesApi::class)

package com.sosauce.cinnamon.data.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.sosauce.cinnamon.data.telephony.CuteTelephonyManager
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

        try {
            if (intent == null) {
                return START_NOT_STICKY
            }

            val number = Uri.decode(
                intent.dataString!!.removePrefix("sms:").removePrefix("smsto:").removePrefix("mms")
                    .removePrefix("mmsto:").trim()
            )
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrEmpty()) {
                val addresses = listOf(number)

                GlobalScope.launch(Dispatchers.IO) {
                    telephonyManager.sendMessage(
                        addresses = addresses,
                        message = text,
                        attachments = emptyList()
                    )
                }

            }
        } catch (_: Exception) {
        }

        return super.onStartCommand(intent, flags, startId)
    }

}