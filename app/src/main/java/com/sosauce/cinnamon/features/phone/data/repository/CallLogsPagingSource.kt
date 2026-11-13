package com.sosauce.cinnamon.features.phone.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sosauce.cinnamon.features.phone.data.model.GroupedCallLog
import kotlinx.coroutines.CancellationException

/**
 * Paging over call logs matching [selection], keyed by cursor row.
 * Keys always sit on a group boundary, so runs of subsequent calls are never split between pages.
 *
 * @param onLoaded called every time a page load finishes (successfully or not)
 */
class CallLogsPagingSource(
    private val callLogsRepository: CallLogsRepository,
    private val selection: String?,
    private val newestFirst: Boolean,
    private val groupSubsequent: Boolean,
    private val onLoaded: () -> Unit = {}
) : PagingSource<Int, GroupedCallLog>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GroupedCallLog> {
        val key = params.key ?: 0

        return try {
            val page = callLogsRepository.getCallLogsPage(
                selection = selection,
                newestFirst = newestFirst,
                groupSubsequent = groupSubsequent,
                fromRow = key,
                limit = params.loadSize,
                backwards = params is LoadParams.Prepend,
                // a refresh key comes from the previous generation, it may now be in the middle of a run
                alignToGroup = params is LoadParams.Refresh
            )
            onLoaded()

            LoadResult.Page(
                data = page.logs,
                prevKey = if (page.startRow <= 0 || page.logs.isEmpty()) null else page.startRow,
                nextKey = if (page.endRow >= page.totalRows || page.logs.isEmpty()) null else page.endRow
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onLoaded() // don't leave the screen loading forever
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GroupedCallLog>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestItemToPosition(anchor)?.startRow
    }
}
