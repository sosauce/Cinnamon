package com.sosauce.cinnamon.features.contacts.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sosauce.cinnamon.features.contacts.data.model.CuteContactEntity
import kotlinx.coroutines.CancellationException
import kotlin.math.max

/**
 * Offset-based paging over contacts sorted by name, restricted by [extraSelection].
 *
 * @param onLoaded called every time a page load finishes (successfully or not)
 */
class ContactsPagingSource(
    private val contactsRepository: ContactsRepository,
    private val extraSelection: String?,
    private val ascending: Boolean,
    private val onLoaded: () -> Unit = {}
) : PagingSource<Int, CuteContactEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CuteContactEntity> {
        val key = params.key ?: 0

        val (offset, limit) = when (params) {
            is LoadParams.Prepend -> {
                val start = max(0, key - params.loadSize)
                start to (key - start)
            }

            else -> max(0, key) to params.loadSize
        }

        return try {
            val page = contactsRepository.getContactsPage(extraSelection, ascending, offset, limit)
            val loadedEnd = offset + page.contacts.size
            onLoaded()

            LoadResult.Page(
                data = page.contacts,
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

    override fun getRefreshKey(state: PagingState<Int, CuteContactEntity>): Int? {
        val anchor = state.anchorPosition ?: return null
        return max(0, anchor - state.config.initialLoadSize / 2)
    }
}
