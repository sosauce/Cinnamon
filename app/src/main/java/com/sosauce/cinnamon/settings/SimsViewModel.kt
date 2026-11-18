package com.sosauce.cinnamon.settings

import android.telecom.PhoneAccountHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.repository.SimsRepository
import kotlinx.coroutines.launch

class SimsViewModel(
    private val simsRepository: SimsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    fun fetchSims() = simsRepository.fetchSims()

    fun fetchPhoneHandles() = simsRepository.fetchPhoneHandles()


    fun fetchLatestDefaultPhoneHandle() = userPreferences.getDefaultPhoneHandle()
    fun saveDefaultPhoneHandle(handle: PhoneAccountHandle) {
        viewModelScope.launch {
            userPreferences.saveDefaultPhoneHandleId(handle)
        }
    }

    fun saveAskEveryTimePhoneHandle() {
        viewModelScope.launch {
            userPreferences.saveAskEveryTimePhoneHandle()
        }
    }
}