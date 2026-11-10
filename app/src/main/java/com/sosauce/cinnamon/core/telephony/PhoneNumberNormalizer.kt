package com.sosauce.cinnamon.core.telephony

import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

class PhoneNumberNormalizer(
    private val telephonyManager: TelephonyManager,
    private val locale: Locale = Locale.getDefault()
) {

    /**
     * Formats the desired number to E164 using the SIM's home region, avoiding visited-network parsing while traveling.
     */
    fun formatToE164(number: String): String {
        return PhoneNumberUtils.formatNumberToE164(
            number,
            getIso()
        ) ?: number
    }


    fun getIso(): String {
        val networkIso = telephonyManager.networkCountryIso
        val simIso = telephonyManager.simCountryIso

        return when {
            simIso.isNotEmpty() -> simIso.uppercase()
            networkIso.isNotEmpty() -> networkIso.uppercase()
            else -> locale.country.uppercase()
        }
    }
}
