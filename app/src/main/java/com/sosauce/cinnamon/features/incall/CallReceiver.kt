package com.sosauce.cinnamon.features.incall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sosauce.cinnamon.core.utils.ACCEPT_INCOMING_CALL
import com.sosauce.cinnamon.core.utils.DECLINE_INCOMING_CALL
import com.sosauce.cinnamon.core.utils.HANGUP_ONGOING_CALL
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles the call notification's actions
 */
class CallReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent?) {

        val inCallRepository by inject<InCallRepository>()
        val state = inCallRepository.inCallState.value as? InCallState.Active ?: return

        when (intent?.action) {
            // Telecom holds the current active call itself when answering a waiting call
            ACCEPT_INCOMING_CALL -> state.ringingCall?.let { inCallRepository.answer(it.id) }
            DECLINE_INCOMING_CALL -> state.ringingCall?.let { inCallRepository.reject(it.id) }
            HANGUP_ONGOING_CALL -> inCallRepository.disconnect(state.primaryCall.id)
        }
    }
}
