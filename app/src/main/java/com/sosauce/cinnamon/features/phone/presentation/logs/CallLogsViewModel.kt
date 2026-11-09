@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.phone.presentation.logs

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.phone.data.repository.CallLogsRepository
import com.sosauce.cinnamon.features.phone.domain.CallType
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CallLogsViewModel(
    private val callLogsRepository: CallLogsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val textFieldState = TextFieldState()

    private val searchQuery = snapshotFlow { textFieldState.text.toString() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()

    private val _state = MutableStateFlow(CallLogsState())

    val state = combine(_state, searchQuery) { state, query ->
        state.copy(isSearching = query.isNotEmpty())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CallLogsState()
    )

    val callLogs: Flow<PagingData<CallLogsListItem>> = combine(
        _state.map { it.filter }.distinctUntilChanged(),
        userPreferences.sortLogsAscending,
        userPreferences.groupSubsequentCalls,
        searchQuery
    ) { filter, asc, groupSub, query -> CallLogsParams(filter, asc, groupSub, query) }
        .distinctUntilChanged()
        .flatMapLatest { params ->
            callLogsRepository.pagedCallLogs(
                ascending = params.ascending,
                groupSubsequent = params.groupSubsequent,
                isFiltered = params.query.isNotEmpty() || params.filter != CallLogsFilter.ALL,
                filter = logFilter(params.query, params.filter)
            )
        }
        .map { pagingData ->
            pagingData
                .map<Pair<CuteCallLog2, Int>, CallLogsListItem> { (log, count) -> CallLogsListItem.Log(log, count) }
                .insertSeparators { before, after ->
                    val next = (after as? CallLogsListItem.Log)?.log ?: return@insertSeparators null
                    val previous = (before as? CallLogsListItem.Log)?.log
                    if (previous?.date != next.date) CallLogsListItem.Header(next.date, next.id) else null
                }
        }
        .cachedIn(viewModelScope)

    private fun logFilter(query: String, filter: CallLogsFilter): (CuteCallLog2) -> Boolean = { log ->
        log.displayName.contains(query, true) && when (filter) {
            CallLogsFilter.ALL -> true
            CallLogsFilter.CONTACTS -> log.displayName != log.number // I don't know if that's the best way to filter contacts
            CallLogsFilter.INCOMING -> log.callType == CallType.INCOMING || log.callType == CallType.REJECTED
            CallLogsFilter.OUTGOING -> log.callType == CallType.OUTGOING
            CallLogsFilter.MISSED -> log.callType == CallType.MISSED
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
    val filter: CallLogsFilter = CallLogsFilter.ALL,
    val isSearching: Boolean = false
)

sealed interface CallLogsListItem {
    data class Header(val date: String, val beforeLogId: Long) : CallLogsListItem
    data class Log(val log: CuteCallLog2, val count: Int) : CallLogsListItem
}

private data class CallLogsParams(
    val filter: CallLogsFilter,
    val ascending: Boolean,
    val groupSubsequent: Boolean,
    val query: String
)

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