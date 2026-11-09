package com.sosauce.cinnamon.features.messaging.data.repository

import android.content.Context
import android.provider.Telephony
import com.sosauce.cinnamon.core.repository.ContentPagingSource
import com.sosauce.cinnamon.features.messaging.data.model.CuteConversationEntity

class ConversationsPagingSource(
    context: Context,
    private val repository: ConversationsRepository,
    private val query: String,
    private val excludedThreadIds: Set<Long>
) : ContentPagingSource<CuteConversationEntity>(context, Telephony.Threads.CONTENT_URI, query.isNotBlank()) {

    override suspend fun loadPage(offset: Int, limit: Int) =
        repository.loadConversationsPage(offset, limit, query, excludedThreadIds)
}
