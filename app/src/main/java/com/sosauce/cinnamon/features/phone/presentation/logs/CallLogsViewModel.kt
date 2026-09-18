@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.phone.presentation.logs

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.utils.groupSubsequentlyBy
import com.sosauce.cinnamon.features.phone.data.repository.CallLogsRepository
import com.sosauce.cinnamon.features.phone.domain.CallType
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
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
import kotlin.time.Duration.Companion.milliseconds

class CallLogsViewModel(
    private val callLogsRepository: CallLogsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val textFieldState = TextFieldState()
    private val _state = MutableStateFlow(
        CallLogsState(
            isLoading = true
        )
    )

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {

            combine(
                callLogsRepository.fetchLatestCallLog(),
                state.mapLatest { it.filter }.distinctUntilChanged(),
                userPreferences.sortLogsAscending,
                userPreferences.groupSubsequentCalls,
                snapshotFlow { textFieldState.text }.debounce(250.milliseconds)
            ) { logs, filter, asc, groupSub, searchQuery ->

                val filteredLogs = logs.fastFilter { it.displayName.contains(searchQuery, true) }
                    .fastFilter { log ->
                        when (filter) {
                            CallLogsFilter.ALL -> true
                            CallLogsFilter.CONTACTS -> log.displayName != log.number // I don't know if that's the best way to filter contacts
                            CallLogsFilter.INCOMING -> log.callType == CallType.INCOMING || log.callType == CallType.REJECTED
                            CallLogsFilter.OUTGOING -> log.callType == CallType.OUTGOING
                            CallLogsFilter.MISSED -> log.callType == CallType.MISSED
                        }
                    }.apply {
                        if (!asc) reversed()
                    }

                val groupedLogs = if (groupSub) {
                    filteredLogs.groupSubsequentlyBy { it.number }
                } else filteredLogs.fastMap { it to 1 }


                groupedLogs.groupBy { (calls, _) -> calls.date }

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
                    callLogsRepository.deleteCallLog(action.ids)
                }
            }
        }
    }

}

data class CallLogsState(
    val isLoading: Boolean = false,
    val callLogs: Map<String, GroupedCalls> = emptyMap(),
    val filter: CallLogsFilter = CallLogsFilter.ALL,
    val isSearching: Boolean = false
)

typealias GroupedCalls = List<Pair<CuteCallLog2, Int>>

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