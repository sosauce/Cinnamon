package com.sosauce.cinnamon.data.managers

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.util.Log

@SuppressLint("MissingPermission")
class CuteSubscriptionsManager(
    private val context: Context
) {

    private val subscriptionManager =
        (context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager)
    val availableSubscriptions = subscriptionManager.activeSubscriptionInfoList
    val currentSimForSms = SubscriptionManager.getDefaultSmsSubscriptionId()

    init {
        Log.d("default sim for sms", currentSimForSms.toString())
    }

    fun changeDefaultSim() {
    }
}