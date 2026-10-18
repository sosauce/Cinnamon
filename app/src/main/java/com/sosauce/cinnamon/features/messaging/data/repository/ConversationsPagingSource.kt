@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.messaging.data.repository

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

class ConversationsPagingSource(
    private val context: Context,
    private val query: String
) : PagingSource<Int, CuteConversation>() {

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            invalidate()
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            Telephony.Threads.CONTENT_URI,
            true,
            observer
        )

        registerInvalidatedCallback {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CuteConversation> {
        val pageNumber = params.key ?: 0
        val pageSize = params.loadSize
        val offset = pageNumber * pageSize
        //TODO
        return LoadResult.Page(emptyList(), 0, 0)
    }

    override fun getRefreshKey(state: PagingState<Int, CuteConversation>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}