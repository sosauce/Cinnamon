package com.sosauce.cinnamon.features.messaging.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sosauce.cinnamon.core.utils.offsetRefreshKey
import com.sosauce.cinnamon.core.utils.toOffsetAndLimit
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import kotlinx.coroutines.CancellationException
import kotlin.math.max

/**
 * Offset-based paging over a thread's SMS + MMS, newest first.
 *
 * Reports itemsBefore/itemsAfter so placeholders are enabled, which lets the UI
 * jump straight to any position (e.g. a search result) and have that page loaded.
 */
class ThreadMessagesPagingSource(
    private val messagesRepository: MessagesRepository,
    private val threadId: Long
) : PagingSource<Int, CuteMessage>() {

    override val jumpingSupported: Boolean = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CuteMessage> {
        val (offset, limit) = params.toOffsetAndLimit()

        return try {
            var start = offset
            var page = messagesRepository.getMessagesPage(threadId, start, limit)
            val total = page.totalCount

            if (start > 0 && start >= total) {
                // thread shrank under us (deletion)
                if (params !is LoadParams.Refresh) return LoadResult.Invalid()
                // refresh anchor is now past the end, load the oldest messages instead
                start = max(0, total - limit)
                page = messagesRepository.getMessagesPage(threadId, start, limit)
            }

            val loadedEnd = start + page.messages.size

            LoadResult.Page(
                data = page.messages,
                prevKey = if (start == 0) null else start,
                nextKey = if (loadedEnd >= total) null else loadedEnd,
                itemsBefore = start,
                itemsAfter = max(0, total - loadedEnd)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CuteMessage>): Int? =
        state.offsetRefreshKey()
}
