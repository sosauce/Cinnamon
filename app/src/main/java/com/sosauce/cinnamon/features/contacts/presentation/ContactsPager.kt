@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import android.provider.ContactsContract
import androidx.compose.ui.util.fastMap
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.sosauce.cinnamon.features.contacts.data.model.CuteContactEntity
import com.sosauce.cinnamon.features.contacts.data.model.toDomain
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsPagingSource
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Data for [contactsList]: unpaged favorites + paged non favorite contacts with letter headers,
 * both restricted by [selection] and refreshed when contacts change.
 *
 * @param selection extra SQL selection on [ContactsContract.Contacts], null for every contact
 */
class ContactsPager(
    private val contactsRepository: ContactsRepository,
    scope: CoroutineScope,
    selection: Flow<String?>,
    sortAscending: Flow<Boolean>
) {

    private val query = combine(selection, sortAscending, ::Pair).distinctUntilChanged()

    // true once the first page of contacts has been loaded, never goes back to false
    // so later refreshes (contact changes, searching) keep the list visible
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /**
     * Favorite contacts, null until loaded
     */
    val favorites: StateFlow<List<CuteContact>?> = query
        .flatMapLatest { (selection, ascending) ->
            contactsRepository
                .observeContactChanges()
                .mapLatest {
                    contactsRepository.getContactsPage(
                        extraSelection = selection.and("${ContactsContract.Contacts.STARRED} = 1"),
                        ascending = ascending,
                        offset = 0,
                        limit = Int.MAX_VALUE
                    ).contacts.fastMap { it.toDomain() }
                }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    private var pagingSourceFactory: InvalidatingPagingSourceFactory<Int, CuteContactEntity>? = null

    /**
     * Non favorite contacts, paged, with a letter header before each group.
     */
    val contacts: Flow<PagingData<ContactsListItem>> = query
        .flatMapLatest { (selection, ascending) ->
            val factory = InvalidatingPagingSourceFactory {
                ContactsPagingSource(
                    contactsRepository = contactsRepository,
                    extraSelection = selection.and("${ContactsContract.Contacts.STARRED} = 0"),
                    ascending = ascending,
                    onLoaded = { _loaded.value = true }
                )
            }
            pagingSourceFactory = factory

            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    initialLoadSize = PAGE_SIZE,
                    prefetchDistance = PAGE_SIZE / 2,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = factory
            ).flow.map { pagingData ->
                pagingData
                    .map { ContactsListItem.Contact(it.toDomain()) }
                    .insertSeparators<ContactsListItem.Contact, ContactsListItem> { before, after ->
                        val next = after?.contact ?: return@insertSeparators null
                        val letter = next.headerLetter

                        if (before?.contact?.headerLetter != letter) {
                            ContactsListItem.Header(letter, next.id)
                        } else null
                    }
            }
        }
        .cachedIn(scope)

    init {
        scope.launch {
            // first emission is the initial value, the paging source is already fresh
            contactsRepository.observeContactChanges().drop(1).collectLatest {
                pagingSourceFactory?.invalidate()
            }
        }
    }

    private fun String?.and(other: String) = if (this == null) other else "$this AND $other"

    companion object {
        private const val PAGE_SIZE = 50
    }
}

/**
 * SQL selection restricting [ContactsContract.Contacts] to these ids, matches nothing when empty.
 * Ids are numeric, so they can be inlined safely (avoids SQLite's bound-variable limit)
 */
fun Collection<Long>.toContactIdSelection(): String =
    if (isEmpty()) "0" else "${ContactsContract.Contacts._ID} IN (${joinToString(",")})"
