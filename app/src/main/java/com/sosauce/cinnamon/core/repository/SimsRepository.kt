package com.sosauce.cinnamon.core.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.compose.ui.util.fastForEach
import androidx.core.app.ActivityCompat
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import kotlin.compareTo

class SimsRepository(
    private val telecomManager: TelecomManager,
    private val subscriptionManager: SubscriptionManager
) {


    @SuppressLint("MissingPermission")
    fun fetchSims(): List<CuteSimCard> {

        return try {
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
            simCards
        } catch (_: SecurityException) {
            emptyList()
        }

    }

    /**
     * Optimized for subId lookup
     */
    fun simLookup(): Map<Int, CuteSimCard> {

        return try {
            val simCards = mutableMapOf<Int, CuteSimCard>()
            subscriptionManager.activeSubscriptionInfoList?.fastForEach { subInfo ->

                val id = subInfo.subscriptionId

                simCards[id] = CuteSimCard(
                    subId = id,
                    name = subInfo.displayName?.toString() ?: "No name",
                    carrierName = subInfo.carrierName?.toString() ?: "No carrier",
                    color = subInfo.iconTint
                )
            }
            simCards
        } catch (_: SecurityException) {
            emptyMap()
        }
    }


    fun fetchPhoneHandles(): Map<PhoneAccount, PhoneAccountHandle> {
        val map = mutableMapOf<PhoneAccount, PhoneAccountHandle>()
        try {
            telecomManager.callCapablePhoneAccounts?.fastForEach { handle ->
                val account = telecomManager.getPhoneAccount(handle) ?: return@fastForEach
                map[account] = handle
            }
        } catch (_: SecurityException) {}
        return map
    }

    fun hasMultipleSims(): Boolean {
        return try {
            subscriptionManager.activeSubscriptionInfoCount > 1
        } catch (_: SecurityException) { false }
    }


}