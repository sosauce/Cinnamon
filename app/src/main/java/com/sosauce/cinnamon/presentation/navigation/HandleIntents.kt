package com.sosauce.cinnamon.presentation.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sosauce.cinnamon.utils.CuteIntents

fun NavBackStack<NavKey>.handleIntent(intent: Intent?) {
    if (intent == null) return

    when(intent.action) {
        CuteIntents.NOTIFICATION_NAVIGATE_TO_THREAD -> {
            val threadId = intent.getLongExtra("threadId", 0L)
            add(Screen.Conversation(threadId))
        }
        else -> return
    }


}