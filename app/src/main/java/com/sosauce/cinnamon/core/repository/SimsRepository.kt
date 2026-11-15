package com.sosauce.cinnamon.core.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.compose.ui.util.fastForEach
import androidx.core.app.ActivityCompat
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import kotlin.compareTo

class SimsRepository(
    private val context: Context,
    private val telecomManager: TelecomManager,
    private val subscriptionManager: SubscriptionManager,
    private val telephonyManager: TelephonyManager
) {

    /**
     * Resolves the subscription id behind a [PhoneAccountHandle]. The handle id isn't always the
     * subId (some devices use the ICCID), so ask the platform first and fall back to matching.
     */
    @SuppressLint("MissingPermission")
    fun subIdFor(handle: PhoneAccountHandle): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val subId = try {
                telephonyManager.getSubscriptionId(handle)
            } catch (_: Exception) { SubscriptionManager.INVALID_SUBSCRIPTION_ID }
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return subId
        }
        handle.id.toIntOrNull()?.let { return it }
        return try {
            subscriptionManager.activeSubscriptionInfoList
                ?.firstOrNull { !it.iccId.isNullOrEmpty() && it.iccId == handle.id }
                ?.subscriptionId
        } catch (_: SecurityException) { null }
    }

    /**
     * key = PhoneAccountHandle id (as stored in CallLog.Calls.PHONE_ACCOUNT_ID), value = subId
     */
    @SuppressLint("MissingPermission")
    fun accountIdToSubId(): Map<String, Int> {
        return try {
            telecomManager.callCapablePhoneAccounts.orEmpty()
                .mapNotNull { handle -> subIdFor(handle)?.let { handle.id to it } }
                .toMap()
        } catch (_: SecurityException) { emptyMap() }
    }


    @SuppressLint("MissingPermission")
    fun fetchSims(): List<CuteSimCard> {

        return try {
            val simCards = mutableListOf<CuteSimCard>()
            subscriptionManager.activeSubscriptionInfoList?.fastForEach { subInfo ->
                simCards.add(
                    CuteSimCard(
                        subId = subInfo.subscriptionId,
                        name = subInfo.displayName?.toString() ?: context.getString(R.string.no_sim_name),
                        carrierName = subInfo.carrierName?.toString() ?: context.getString(R.string.no_carrier),
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
                    name = subInfo.displayName?.toString() ?: context.getString(R.string.no_sim_name),
                    carrierName = subInfo.carrierName?.toString() ?: context.getString(R.string.no_carrier),
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