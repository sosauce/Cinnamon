@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.sosauce.cinnamon.features.phone.presentation.logs

import android.provider.CallLog
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.features.phone.data.model.GroupedCallLog
import com.sosauce.cinnamon.features.phone.data.repository.CallLogsPagingSource
import com.sosauce.cinnamon.features.phone.data.repository.CallLogsRepository
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CallLogsViewModel(
    private val callLogsRepository: CallLogsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val textFieldState = TextFieldState()

    private val filter = MutableStateFlow(CallLogsFilter.ALL)

    private val searchQuery = snapshotFlow { textFieldState.text.toString().trim() }
        .debounce(250.milliseconds)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /**
     * Ids of the call logs matching the search query, null when not searching
     */
    private val searchResults: Flow<Set<Long>?> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(null)
            } else {
                // refresh results when the call log changes
                callLogsRepository
                    .observeCallLogChanges()
                    .mapLatest { callLogsRepository.searchCallLogIds(query) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val selection: Flow<String?> = combine(searchResults, filter) { results, filter ->
        listOfNotNull(
            filter.toSelection(),
            results?.let { ids ->
                if (ids.isEmpty()) "0" // nothing matches
                // ids are numeric, so they can be inlined safely (avoids SQLite's bound-variable limit)
                else "${CallLog.Calls._ID} IN (${ids.joinToString(",")})"
            }
        ).joinToString(" AND ").ifEmpty { null }
    }.distinctUntilChanged()

    private var pagingSourceFactory: InvalidatingPagingSourceFactory<Int, GroupedCallLog>? = null

    // true once the first page of call logs has been loaded, never goes back to false
    // so later refreshes (new calls, searching) keep the list visible
    private val callLogsLoaded = MutableStateFlow(false)

    /**
     * Call logs, paged, with a date header before each day.
     */
    val callLogs: Flow<PagingData<CallLogsListItem>> = combine(
        selection,
        userPreferences.sortLogsAscending,
        userPreferences.groupSubsequentCalls
    ) { selection, ascending, groupSubsequent -> Triple(selection, ascending, groupSubsequent) }
        .distinctUntilChanged()
        .flatMapLatest { (selection, ascending, groupSubsequent) ->
            val factory = InvalidatingPagingSourceFactory {
                CallLogsPagingSource(
                    callLogsRepository = callLogsRepository,
                    selection = selection,
                    // "ascending" has always meant newest first here
                    newestFirst = ascending,
                    groupSubsequent = groupSubsequent,
                    onLoaded = { callLogsLoaded.value = true }
                )
            }
            pagingSourceFactory = factory

            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    initialLoadSize = PAGE_SIZE,
                    prefetchDistance = PAGE_SIZE / 2,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = factory
            ).flow.map { pagingData ->
                pagingData
                    .map { CallLogsListItem.Log(it.log, it.count) }
                    .insertSeparators<CallLogsListItem.Log, CallLogsListItem> { before, after ->
                        val next = after?.log ?: return@insertSeparators null

                        if (before?.log?.date != next.date) {
                            CallLogsListItem.Header(next.date, next.id)
                        } else null
                    }
            }
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            // first emission is the initial value, the paging source is already fresh
            callLogsRepository.observeCallLogChanges().drop(1).collectLatest {
                pagingSourceFactory?.invalidate()
            }
        }
    }

    val state = combine(
        filter,
        searchQuery,
        callLogsLoaded
    ) { filter, query, loaded ->
        CallLogsState(
            isLoading = !loaded,
            filter = filter,
            isSearching = query.isNotEmpty()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CallLogsState(isLoading = true)
    )

    fun handleDialerAction(action: DialerAction) {
        when (action) {
            is DialerAction.ChangeFilter -> filter.value = action.filter
            is DialerAction.ChangeSort -> Unit
            is DialerAction.DeleteLogs -> viewModelScope.launch(Dispatchers.IO) {
                callLogsRepository.deleteCallLog(action.ids)
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 50
    }
}

/**
 * SQL selection matching this filter, null for [CallLogsFilter.ALL]
 */
private fun CallLogsFilter.toSelection(): String? = when (this) {
    CallLogsFilter.ALL -> null
    CallLogsFilter.CONTACTS -> "${CallLog.Calls.CACHED_NAME} IS NOT NULL AND ${CallLog.Calls.CACHED_NAME} != ''"
    // every type that isn't outgoing or missed shows up as incoming/rejected
    CallLogsFilter.INCOMING -> "${CallLog.Calls.TYPE} NOT IN (${CallLog.Calls.OUTGOING_TYPE}, ${CallLog.Calls.MISSED_TYPE})"
    CallLogsFilter.OUTGOING -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.OUTGOING_TYPE}"
    CallLogsFilter.MISSED -> "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE}"
}

data class CallLogsState(
    val isLoading: Boolean = false,
    val filter: CallLogsFilter = CallLogsFilter.ALL,
    val isSearching: Boolean = false
)

sealed interface CallLogsListItem {
    val key: Any

    data class Header(val date: String, val beforeLogId: Long) : CallLogsListItem {
        override val key = "header_$beforeLogId"
    }

    data class Log(val log: CuteCallLog, val count: Int) : CallLogsListItem {
        override val key = log.id
    }
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
