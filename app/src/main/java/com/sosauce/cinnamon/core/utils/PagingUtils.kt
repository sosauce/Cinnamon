package com.sosauce.cinnamon.core.utils

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.math.max

/**
 * Paging config shared by the main lists (contacts, conversations, call logs)
 */
fun listPagingConfig(pageSize: Int) = PagingConfig(
    pageSize = pageSize,
    initialLoadSize = pageSize,
    prefetchDistance = pageSize / 2,
    enablePlaceholders = false
)

/**
 * Offset and limit to query for an offset-keyed [PagingSource] load
 */
fun PagingSource.LoadParams<Int>.toOffsetAndLimit(): Pair<Int, Int> {
    val key = key ?: 0

    return when (this) {
        is PagingSource.LoadParams.Prepend -> {
            val start = max(0, key - loadSize)
            start to (key - start)
        }

        else -> max(0, key) to loadSize
    }
}

/**
 * Refresh key of an offset-keyed [PagingSource], centered around the anchor position
 */
fun <Value : Any> PagingState<Int, Value>.offsetRefreshKey(): Int? {
    val anchor = anchorPosition ?: return null
    return max(0, anchor - config.initialLoadSize / 2)
}
