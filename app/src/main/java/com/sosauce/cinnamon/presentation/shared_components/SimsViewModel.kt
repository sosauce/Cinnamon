package com.sosauce.cinnamon.presentation.shared_components

import android.telecom.PhoneAccountHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.datastore.UserPreferences
import com.sosauce.cinnamon.domain.repository.SimsRepository
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
}