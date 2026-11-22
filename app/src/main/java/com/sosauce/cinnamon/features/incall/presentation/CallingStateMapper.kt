package com.sosauce.cinnamon.features.incall.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.PhoneAccountHandle
import android.telephony.SubscriptionManager
import com.sosauce.cinnamon.core.repository.SimsRepository
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.features.incall.CallAudio
import com.sosauce.cinnamon.features.incall.CallInfo
import com.sosauce.cinnamon.features.incall.CallState
import com.sosauce.cinnamon.features.incall.InCallRepository
import com.sosauce.cinnamon.features.incall.InCallState
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

/**
 * Turns [InCallRepository]'s state into the [CallingState] shown by the call screen
 */
class CallingStateMapper(
    private val context: Context,
    private val inCallRepository: InCallRepository,
    private val simsRepository: SimsRepository
) {

    private val sims = mutableMapOf<PhoneAccountHandle, CuteSimCard>()

    // Re-maps every second, so the call timers move
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1.seconds)
        }
    }

    val callingState: Flow<CallingState> = combine(
        inCallRepository.inCallState,
        inCallRepository.callAudio,
        ticker
    ) { state, audio, _ -> map(state, audio) }
        .distinctUntilChanged()

    fun currentCallingState(): CallingState =
        map(inCallRepository.inCallState.value, inCallRepository.callAudio.value)

    private fun map(
        state: InCallState,
        audio: CallAudio,
        now: Long = System.currentTimeMillis()
    ): CallingState {
        if (state !is InCallState.Active) return CallingState(callState = CallUiState.ENDED)

        val primary = state.primaryCall
        return CallingState(
            callState = primary.state.toUiState(),
            number = primary.number?.beautifyNumber().orEmpty(),
            displayName = primary.contactName,
            isMuted = audio.isMuted,
            isHolding = primary.state == CallState.HOLDING,
            timeSpentInCall = primary.elapsedSeconds(now),
            availableAudioRoutes = audio.availableRoutes,
            currentAudioRoute = audio.currentRoute,
            photo = primary.contactPhotoUri,
            activeSim = simFor(primary.phoneAccountHandle),
            isHdAudio = primary.capabilities.isHd,
            isWifiCall = primary.capabilities.isWifi,
            otherCall = state.secondaryCall?.let { other ->
                OtherCall(
                    number = other.number?.beautifyNumber().orEmpty(),
                    displayName = other.contactName,
                    isHolding = other.state == CallState.HOLDING,
                    timeSpentInCall = other.elapsedSeconds(now)
                )
            }
        )
    }

    private fun CallState.toUiState(): CallUiState = when (this) {
        CallState.RINGING -> CallUiState.RINGING
        CallState.ACTIVE, CallState.HOLDING -> CallUiState.ONGOING
        else -> CallUiState.DIALING
    }

    private fun CallInfo.elapsedSeconds(now: Long): Long {
        if (state != CallState.ACTIVE && state != CallState.HOLDING) return 0L
        val connectTime = connectTimeMillis ?: return 0L
        return ((now - connectTime) / 1000).coerceAtLeast(0L)
    }

    @SuppressLint("MissingPermission")
    private fun simFor(handle: PhoneAccountHandle?): CuteSimCard {
        if (handle == null) return CuteSimCard()
        return sims.getOrPut(handle) {
            val subId = simsRepository.subIdFor(handle) ?: -1
            val activeSubInfo = try {
                context.getSystemService(SubscriptionManager::class.java).getActiveSubscriptionInfo(subId)
            } catch (_: SecurityException) {
                null
            }
            CuteSimCard(
                subId = activeSubInfo?.subscriptionId ?: subId,
                name = activeSubInfo?.displayName?.toString() ?: "SIM $subId",
                carrierName = activeSubInfo?.carrierName?.toString() ?: "",
                color = activeSubInfo?.iconTint ?: 0
            )
        }
    }
}
