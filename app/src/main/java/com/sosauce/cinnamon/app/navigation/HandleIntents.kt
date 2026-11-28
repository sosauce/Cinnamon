package com.sosauce.cinnamon.app.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sosauce.cinnamon.core.utils.CuteIntents
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate

fun NavBackStack<NavKey>.handleIntent(
    context: Context,
    intent: Intent?
) {
    if (intent == null) return

    when (intent.action) {
        CuteIntents.NOTIFICATION_NAVIGATE_TO_THREAD -> {
            val threadId = intent.getLongExtra("threadId", 0L)
            add(Screen.ConversationDetails(threadId))
        }

        CuteIntents.SHORTCUT_MESSAGES -> add(Screen.Conversations)
        CuteIntents.SHORTCUT_CONTACTS -> add(Screen.Contacts)
        CuteIntents.SHORTCUT_DIALPAD -> add(Screen.Dialer())
        CuteIntents.SHORTCUT_CONTACT_DETAILS -> add(Screen.ContactDetails(intent.getLongExtra("contactId", 0L)))

        Intent.ACTION_DIAL -> {
            val number = intent.data?.schemeSpecificPart.orEmpty().replace('%', ' ')
            add(Screen.Dialer(number))
        }

        Intent.ACTION_SENDTO -> {

            val threadId = intent.data
                ?.schemeSpecificPart
                .orEmpty()
                .getThreadIdOrCreate(context)
            val message = intent.getStringExtra("sms_body") ?: ""
            add(Screen.ConversationDetails(threadId, message))
        }

        else -> return
    }


}