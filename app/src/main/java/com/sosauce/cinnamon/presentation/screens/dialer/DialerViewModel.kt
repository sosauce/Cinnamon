@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.presentation.screens.dialer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sosauce.cinnamon.domain.model.CuteCallLog
import com.sosauce.cinnamon.domain.repository.DialerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DialerViewModel(
    private val dialerRepository: DialerRepository
): ViewModel() {

    private val _state = MutableStateFlow(DialerState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {

            val callLogs = dialerRepository.callLogsObserver
                .flatMapLatest { dialerRepository.fetchCallLogsPagination() }
                .cachedIn(viewModelScope)

            _state.update {
                it.copy(
                    isLoading = false,
                    callLogs = callLogs
                )
            }
        }
    }

    fun deleteCallLogs(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            dialerRepository.deleteCallLog(ids)
        }
    }

}

data class DialerState(
    val isLoading: Boolean = false,
    val callLogs: Flow<PagingData<CuteCallLog>> = emptyFlow()
)