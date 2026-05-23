@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.presentation.screens.dialer

import android.provider.CallLog
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.data.datastore.UserPreferences
import com.sosauce.cinnamon.domain.model.CuteCallLog
import com.sosauce.cinnamon.domain.repository.DialerRepository
import com.sosauce.cinnamon.utils.copyMutate
import com.sosauce.cinnamon.utils.groupSubsequentlyBy
import com.sosauce.cinnamon.utils.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DialerViewModel(
    private val dialerRepository: DialerRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val textFieldState = TextFieldState()
    private val _state = MutableStateFlow(
        DialerState(
            isLoading = true,
            textFieldState = textFieldState
        )
    )

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {

            combine(
                dialerRepository.fetchLatestCallLog(),
                state.mapLatest { it.filter }.distinctUntilChanged(),
                userPreferences.sortLogsAscending,
                userPreferences.groupSubsequentCalls,
                snapshotFlow { textFieldState.text }.debounce(250)
            ) { logs, filter, asc, groupSub, searQuery ->

                val filteredLogs = logs.fastFilter { it.cachedName.contains(searQuery, true) }
                    .fastFilter { log ->
                        when (filter) {
                            CallLogsFilter.ALL -> true
                            CallLogsFilter.CONTACTS -> log.cachedName != log.rawNumber // idk if that's the best way to filter contacts
                            CallLogsFilter.INCOMING -> log.callType == CallLog.Calls.INCOMING_TYPE || log.callType == CallLog.Calls.REJECTED_TYPE
                            CallLogsFilter.OUTGOING -> log.callType == CallLog.Calls.OUTGOING_TYPE
                            CallLogsFilter.MISSED -> log.callType == CallLog.Calls.MISSED_TYPE
                        }
                    }.copyMutate {
                        if (!asc) reverse()
                    }

                val groupedLogs = if (groupSub) {
                    filteredLogs.groupSubsequentlyBy { it.rawNumber }
                } else filteredLogs.fastMap { it to 1 }


                groupedLogs.groupBy { (calls, _) -> calls.date.toDate() }

            }.flowOn(Dispatchers.Default).collectLatest { logs ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        callLogs = logs,
                        isSearching = textFieldState.text.isNotEmpty()
                    )
                }
            }

        }
    }

    fun handleDialerAction(action: DialerAction) {
        when (action) {
            is DialerAction.ChangeFilter -> {
                _state.update {
                    it.copy(
                        filter = action.filter
                    )
                }
            }

            is DialerAction.ChangeSort -> {}
            is DialerAction.DeleteLogs -> {
                viewModelScope.launch(Dispatchers.IO) {
                    dialerRepository.deleteCallLog(action.ids)
                }
            }
        }
    }

}

data class DialerState(
    val isLoading: Boolean = false,
    val callLogs: Map<String, GroupedCalls> = emptyMap(),
    val filter: CallLogsFilter = CallLogsFilter.ALL,
    val textFieldState: TextFieldState = TextFieldState(),
    val isSearching: Boolean = false
)

typealias GroupedCalls = List<Pair<CuteCallLog, Int>>

sealed interface DialerAction {
    data class ChangeFilter(val filter: CallLogsFilter) : DialerAction
    data object ChangeSort : DialerAction

    data class DeleteLogs(val ids: List<Long>) : DialerAction
}


enum class CallLogsFilter {
    ALL,
    CONTACTS,
    INCOMING,
    OUTGOING,
    MISSED
}