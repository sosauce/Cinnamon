package com.sosauce.cinnamon.features.messaging.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sosauce.cinnamon.core.utils.offsetRefreshKey
import com.sosauce.cinnamon.core.utils.toOffsetAndLimit
import com.sosauce.cinnamon.features.messaging.data.model.CuteConversationEntity
import kotlinx.coroutines.CancellationException

/**
 * Offset-based paging over conversations (newest first), restricted by [extraSelection].
 *
 * @param onLoaded called every time a page load finishes (successfully or not)
 */
class ConversationsPagingSource(
    private val conversationsRepository: ConversationsRepository,
    private val extraSelection: String?,
    private val onLoaded: () -> Unit = {}
) : PagingSource<Int, CuteConversationEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CuteConversationEntity> {
        val (offset, limit) = params.toOffsetAndLimit()

        return try {
            val page = conversationsRepository.getConversationsPage(extraSelection, offset, limit)
            val loadedEnd = offset + page.messages.size
            onLoaded()

            LoadResult.Page(
                data = page.messages,
                prevKey = if (offset == 0) null else offset,
                nextKey = if (loadedEnd >= page.totalCount) null else loadedEnd
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onLoaded() // don't leave the screen loading forever
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CuteConversationEntity>): Int? =
        state.offsetRefreshKey()
}
