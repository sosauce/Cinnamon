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
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CallLogsViewModel(
    private val callLogsRepository: CallLogsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val textFieldState = TextFieldState()
    private val _state = MutableStateFlow(CallLogsState(isLoading = true))

    val state = combine(
        _state,
        callLogsRepository.fetchLatestCallLog(),
        userPreferences.sortLogsAscending,
        userPreferences.groupSubsequentCalls,
        snapshotFlow { textFieldState.text.toString() }.debounce(250.milliseconds).distinctUntilChanged()
    ) { current, logs, ascending, groupSubsequent, query ->
        val sortedLogs = logs
            .fastFilter { it.displayName.contains(query, true) }
            .fastFilter { log ->
                when (current.filter) {
                    CallLogsFilter.ALL -> true
                    CallLogsFilter.CONTACTS -> log.displayName != log.number
                    CallLogsFilter.INCOMING -> log.callType == CallType.INCOMING || log.callType == CallType.REJECTED
                    CallLogsFilter.OUTGOING -> log.callType == CallType.OUTGOING
                    CallLogsFilter.MISSED -> log.callType == CallType.MISSED
                }
            }
            .let { if (ascending) it else it.reversed() }

        val groupedLogs = if (groupSubsequent) {
            sortedLogs.groupSubsequentlyBy { it.number }
        } else {
            sortedLogs.fastMap { it to 1 }
        }

        val items = buildList {
            var currentDate: String? = null
            groupedLogs.forEach { (log, count) ->
                if (log.date != currentDate) {
                    add(CallLogsListItem.Header(log.date, log.id))
                    currentDate = log.date
                }
                add(CallLogsListItem.Log(log, count))
            }
        }

        current.copy(
            isLoading = false,
            callLogs = items,
            isSearching = query.isNotEmpty()
        )
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CallLogsState(isLoading = true)
    )

    fun handleDialerAction(action: DialerAction) {
        when (action) {
            is DialerAction.ChangeFilter -> _state.update { it.copy(filter = action.filter) }
            is DialerAction.ChangeSort -> Unit
            is DialerAction.DeleteLogs -> viewModelScope.launch(Dispatchers.IO) {
                callLogsRepository.deleteCallLog(action.ids)
            }
        }
    }
}

data class CallLogsState(
    val isLoading: Boolean = false,
    val callLogs: List<CallLogsListItem> = emptyList(),
    val filter: CallLogsFilter = CallLogsFilter.ALL,
    val isSearching: Boolean = false
)

sealed interface CallLogsListItem {
    data class Header(val date: String, val beforeLogId: Long) : CallLogsListItem
    data class Log(val log: CuteCallLog, val count: Int) : CallLogsListItem
}

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
