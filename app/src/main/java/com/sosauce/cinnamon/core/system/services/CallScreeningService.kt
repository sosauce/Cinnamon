package com.sosauce.cinnamon.core.system.services

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection

class CuteCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            respondToCall(callDetails, allowCallResponse())
            return
        }

        val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        if (isIncoming) {
            when (callDetails.callerNumberVerificationStatus) {
                Connection.VERIFICATION_STATUS_FAILED -> {
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
                    val response = allowCallResponse()
                    respondToCall(callDetails, response)
                }

                else -> {
                    val response = allowCallResponse()
                    respondToCall(callDetails, response)
                }
            }
        } else {
            val response = allowCallResponse()
            respondToCall(callDetails, response)
        }
    }

    private fun allowCallResponse(): CallResponse = CallResponse.Builder().build()

}
