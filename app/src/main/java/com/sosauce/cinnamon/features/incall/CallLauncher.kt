package com.sosauce.cinnamon.features.incall

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.net.toUri
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.repository.SimsRepository
import com.sosauce.cinnamon.core.telephony.PhoneNumberNormalizer
import com.sosauce.cinnamon.features.phone.domain.SimChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Places outgoing calls and sends service codes (USSD/MMI). It's NOT responsible for calls once
 * they exist, nor for showing the call UI (CallService does it once Telecom adds the call)
 */
class CallLauncher(
    private val telecomManager: TelecomManager,
    private val userPreferences: UserPreferences,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val simsRepository: SimsRepository
) {

    /**
     * Places a call to [number], or sends it to Telephony if it's a service code (*#06#, *100#...)
     * @param forcedHandle SIM picked by the user after [LaunchCallResult.NeedsSimSelection]
     */
    @SuppressLint("MissingPermission")
    suspend fun launchCall(
        number: String,
        forcedHandle: PhoneAccountHandle? = null
    ): LaunchCallResult {
        return try {
            if (telecomManager.isInCall) return LaunchCallResult.Failed

            val phoneAccountHandle = when (val resolution = resolvePhoneAccountHandle(forcedHandle)) {
                is HandleResolution.Use -> resolution.handle
                is HandleResolution.AskUser -> return LaunchCallResult.NeedsSimSelection(fetchSimChoices())
            }
            val bundle = Bundle().apply {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            }

            if (number.isServiceCode()) {
                val wasMmi = if (phoneAccountHandle != null) {
                    telecomManager.handleMmi(number, phoneAccountHandle)
                } else {
                    telecomManager.handleMmi(number)
                }

                if (wasMmi) {
                    LaunchCallResult.ServiceCodeSent
                } else {
                    val serviceCode = Uri.fromParts(
                        PhoneAccount.SCHEME_TEL,
                        number,
                        null
                    )

                    telecomManager.placeCall(serviceCode, bundle)
                    LaunchCallResult.ServiceCodeSent
                }
            } else {
                val numberUri = Uri.fromParts(
                    PhoneAccount.SCHEME_TEL,
                    phoneNumberNormalizer.formatToE164(number),
                    null
                )
                telecomManager.placeCall(numberUri, bundle)
                LaunchCallResult.Started
            }
        } catch (_: SecurityException) {
            LaunchCallResult.NotAllowed
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            LaunchCallResult.Failed
        }
    }

    /**
     * USSD/MMI codes start with * or # and end with #.
     */
    private fun String.isServiceCode(): Boolean =
        length > 1 && (startsWith('*') || startsWith('#')) && endsWith('#')

    @SuppressLint("MissingPermission")
    private suspend fun resolvePhoneAccountHandle(forcedHandle: PhoneAccountHandle?): HandleResolution {
        if (forcedHandle != null) return HandleResolution.Use(forcedHandle)

        return try {
            val accounts = telecomManager.callCapablePhoneAccounts.orEmpty()
            if (accounts.size <= 1) {
                return HandleResolution.Use(
                    accounts.firstOrNull()
                        ?: telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
                )
            }
            val savedHandle = userPreferences.getDefaultPhoneHandle().first()
            if (savedHandle != null) HandleResolution.Use(savedHandle) else HandleResolution.AskUser
        } catch (_: SecurityException) {
            HandleResolution.Use(null)
        }
    }

    private fun fetchSimChoices(): List<SimChoice> {
        return simsRepository.fetchPhoneHandles().map { (account, handle) ->
            SimChoice(
                handle = handle,
                label = account.label?.toString().orEmpty(),
                color = account.highlightColor
            )
        }
    }
}

sealed interface LaunchCallResult {
    data object Started : LaunchCallResult

    /**
     * The number was a service code (USSD/MMI) and was sent to Telephony, no call was placed
     */
    data object ServiceCodeSent : LaunchCallResult
    data object Failed : LaunchCallResult

    /**
     * The system refused the call with a [SecurityException], Cinnamon needs to be the default dialer app
     */
    data object NotAllowed : LaunchCallResult

    /**
     * Nothing was placed, the user has to pick one of [choices], then call [CallLauncher.launchCall] again with it
     */
    data class NeedsSimSelection(val choices: List<SimChoice>) : LaunchCallResult
}

private sealed interface HandleResolution {
    /**
     * @param handle Null lets Telecom pick the account
     */
    data class Use(val handle: PhoneAccountHandle?) : HandleResolution
    data object AskUser : HandleResolution
}
