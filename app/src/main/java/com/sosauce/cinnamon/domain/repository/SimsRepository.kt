package com.sosauce.cinnamon.domain.repository

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.domain.model.CuteSimCard

class SimsRepository(
    private val context: Context,
    private val telecomManager: TelecomManager
) {

    private val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)

    @SuppressLint("MissingPermission")
    fun fetchSims(): List<CuteSimCard> {
        val simCards = mutableListOf<CuteSimCard>()

        subscriptionManager.activeSubscriptionInfoList?.fastForEach { subInfo ->
            simCards.add(
                CuteSimCard(
                    subId = subInfo.subscriptionId,
                    name = subInfo.displayName?.toString() ?: "No name",
                    carrierName = subInfo.carrierName?.toString() ?: "No carrier",
                    color = subInfo.iconTint
                )
            )
        }
        return simCards
    }


    @SuppressLint("MissingPermission")
    fun fetchPhoneHandles(): Map<PhoneAccount, PhoneAccountHandle> {
        val map = mutableMapOf<PhoneAccount, PhoneAccountHandle>()
        telecomManager.callCapablePhoneAccounts?.fastForEach { handle ->

            val account = telecomManager.getPhoneAccount(handle)
            map[account] = handle
        }
        return map
    }


}