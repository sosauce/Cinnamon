package com.sosauce.cinnamon.features.phone.data.repository

import android.content.Context
import android.provider.CallLog
import com.sosauce.cinnamon.core.repository.ContentPagingSource
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2

class CallLogsPagingSource(
    context: Context,
    private val repository: CallLogsRepository,
    private val ascending: Boolean,
    private val groupSubsequent: Boolean,
    isFiltered: Boolean,
    private val filter: (CuteCallLog2) -> Boolean
) : ContentPagingSource<Pair<CuteCallLog2, Int>>(context, CallLog.Calls.CONTENT_URI, isFiltered || groupSubsequent) {

    override suspend fun loadPage(offset: Int, limit: Int) =
        repository.loadCallLogsPage(offset, limit, ascending, groupSubsequent, filter)
}
