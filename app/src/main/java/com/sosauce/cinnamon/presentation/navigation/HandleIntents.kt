package com.sosauce.cinnamon.presentation.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sosauce.cinnamon.utils.CuteIntents
import com.sosauce.cinnamon.utils.getThreadIdOrCreate

fun NavBackStack<NavKey>.handleIntent(
    context: Context,
    intent: Intent?
) {
    if (intent == null) return

    when (intent.action) {
        CuteIntents.NOTIFICATION_NAVIGATE_TO_THREAD -> {
            val threadId = intent.getLongExtra("threadId", 0L)
            add(Screen.Conversation(threadId))
        }

        Intent.ACTION_DIAL -> {
            val number = (intent.data?.toString() ?: "").removePrefix("tel:")
            add(Screen.Dialpad(number))
        }

        Intent.ACTION_SENDTO -> {
            val threadId =
                (intent.data?.toString() ?: "").removePrefix("smsto:").getThreadIdOrCreate(context)
            val message = intent.getStringExtra("sms_body") ?: ""
            add(Screen.Conversation(threadId, message))
        }

        else -> return
    }


}