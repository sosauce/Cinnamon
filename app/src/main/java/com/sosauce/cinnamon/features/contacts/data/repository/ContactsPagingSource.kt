package com.sosauce.cinnamon.features.contacts.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.sosauce.cinnamon.core.repository.ContentPagingSource
import com.sosauce.cinnamon.features.contacts.domain.CuteContact

class ContactsPagingSource(
    context: Context,
    private val repository: ContactsRepository,
    private val ascending: Boolean,
    isFiltered: Boolean,
    private val filter: (CuteContact) -> Boolean
) : ContentPagingSource<CuteContact>(context, ContactsContract.Contacts.CONTENT_URI, isFiltered) {

    override suspend fun loadPage(offset: Int, limit: Int) =
        repository.loadContactsPage(offset, limit, ascending, filter)
}
