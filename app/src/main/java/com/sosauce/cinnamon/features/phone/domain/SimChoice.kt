package com.sosauce.cinnamon.features.phone.domain

import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Immutable

/**
 * A SIM (or other call capable account) the user can pick to place a call with.
 */
@Immutable
data class SimChoice(
    val handle: PhoneAccountHandle,
    val label: String,
    val color: Int
)

/**
 * A call waiting for the user to pick which SIM to use.
 */
@Immutable
data class SimPickerState(
    val number: String,
    val sims: List<SimChoice>
)
