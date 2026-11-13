package com.sosauce.cinnamon.features.phone.data.model

import com.sosauce.cinnamon.features.phone.domain.CuteCallLog

/**
 * A call log, or a run of subsequent calls with the same number (represented by its most recent call)
 *
 * @param count amount of calls in the run, 1 when calls aren't grouped
 * @param startRow cursor position of the run's first row
 */
data class GroupedCallLog(
    val log: CuteCallLog,
    val count: Int,
    val startRow: Int
)

/**
 * A slice of call logs covering the cursor rows [startRow, endRow), out of [totalRows].
 * Runs of grouped calls are never split between pages.
 */
data class CallLogsPage(
    val logs: List<GroupedCallLog>,
    val startRow: Int,
    val endRow: Int,
    val totalRows: Int
)
