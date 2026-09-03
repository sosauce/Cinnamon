package com.sosauce.cinnamon.core.system.services

import android.os.Build
import android.provider.BlockedNumberContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import androidx.annotation.RequiresApi
import androidx.core.net.toUri

@RequiresApi(Build.VERSION_CODES.R)
class CuteCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        // Check blocked numbers first — full dialer behavior
        if (isIncoming) {
            val handle = callDetails.handle?.schemeSpecificPart
            if (!handle.isNullOrBlank() && isBlocked(handle)) {
                val blockedResponse = CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSilenceCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                respondToCall(callDetails, blockedResponse)
                return
            }
        }

        if (isIncoming) {
            when (callDetails.callerNumberVerificationStatus) {
                Connection.VERIFICATION_STATUS_FAILED -> {
                    // Network verification failed, likely an invalid/spam call.
                    val response = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSilenceCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()

                    respondToCall(callDetails, response)
                }

                Connection.VERIFICATION_STATUS_PASSED -> {
                    // Network verification passed, likely a valid call.
                    val response = CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSilenceCall(false)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()

                    respondToCall(callDetails, response)
                }

                else -> {
                    // Network could not perform verification.
                    // This branch matches Connection.VERIFICATION_STATUS_NOT_VERIFIED.
                    val response = CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSilenceCall(false)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()

                    respondToCall(callDetails, response)
                }
            }
        } else {
            // Outgoing — always allow, but ensure we don't block outgoing via screening
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            respondToCall(callDetails, response)
        }
    }

    private fun isBlocked(number: String): Boolean {
        return try {
            val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
            contentResolver.query(
                uri,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                arrayOf(number),
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

}