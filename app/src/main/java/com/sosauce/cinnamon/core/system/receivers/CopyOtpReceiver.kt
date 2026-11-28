package com.sosauce.cinnamon.core.system.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.THREAD_ID
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CopyOtpReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(THREAD_ID, Long.MIN_VALUE)
        val otp = intent.getStringExtra(EXTRA_OTP)
        if (threadId == Long.MIN_VALUE || otp == null) return

        val clip = ClipData.newPlainText("OTP", otp).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.code_copied, Toast.LENGTH_SHORT).show()
        }

        val conversationsRepository by inject<ConversationsRepository>()
        val ioScope by inject<CoroutineScope>()
        val notifManager by inject<NotificationManager>()

        ioScope.launch {
            conversationsRepository.markConversationAsRead(threadId)
            notifManager.cancel(threadId.toInt())
        }
    }

    companion object {
        const val EXTRA_OTP = "com.sosauce.cinnamon.OTP"
    }
}
