package com.sosauce.cinnamon.core.repository

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class ContentPagingSource<T : Any>(
    private val context: Context,
    observedUri: Uri,
    private val isFiltered: Boolean
) : PagingSource<Int, T>() {

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            invalidate()
        }
    }

    init {
        context.contentResolver.registerContentObserver(observedUri, true, observer)

        registerInvalidatedCallback {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    abstract suspend fun loadPage(offset: Int, limit: Int): Pair<List<T>, Int?>

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val key = params.key ?: 0
        val (offset, limit) = when (params) {
            is LoadParams.Prepend -> (key - params.loadSize).coerceAtLeast(0).let { it to key - it }
            else -> key to params.loadSize
        }

        return try {
            val (items, nextOffset) = withContext(Dispatchers.IO) { loadPage(offset, limit) }
            LoadResult.Page(
                data = items,
                prevKey = offset.takeIf { it > 0 && !isFiltered },
                nextKey = if (params is LoadParams.Prepend) offset + limit else nextOffset
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        if (isFiltered) return null
        return state.anchorPosition?.let { anchor ->
            (anchor - state.config.initialLoadSize / 2).coerceAtLeast(0)
        }
    }
}
