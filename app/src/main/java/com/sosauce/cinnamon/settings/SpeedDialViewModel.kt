package com.sosauce.cinnamon.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.datastore.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpeedDialViewModel(
    private val userPreferences: UserPreferences,
    private val numberLookup: NumberLookup
) : ViewModel() {

    val speedDials = userPreferences.speedDials
        .map { dials ->
            dials.mapValues { (_, number) ->
                SpeedDial(
                    number = number,
                    name = runCatching { numberLookup.fetchContactDisplayName(number) }.getOrNull()
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun assign(digit: Int, number: String) {
        viewModelScope.launch { userPreferences.saveSpeedDial(digit, number) }
    }

    fun unassign(digit: Int) {
        viewModelScope.launch { userPreferences.removeSpeedDial(digit) }
    }
}

data class SpeedDial(
    val number: String,
    val name: String?
)
