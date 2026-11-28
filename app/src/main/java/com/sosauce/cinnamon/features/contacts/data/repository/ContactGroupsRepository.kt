@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.contacts.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import androidx.core.content.contentValuesOf
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.contacts.domain.ContactAccount
import com.sosauce.cinnamon.features.contacts.domain.ContactGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class ContactGroupsRepository(
    private val context: Context
) {

    private val resolver get() = context.contentResolver

    fun observeGroups(): Flow<List<ContactGroup>> =
        resolver.observe(Groups.CONTENT_URI)
            .mapLatest { fetchGroups() }
            .flowOn(Dispatchers.IO)

    fun observeMemberships(contactId: Long? = null): Flow<Map<Long, Set<Long>>> =
        resolver.observe(Data.CONTENT_URI)
            .mapLatest { fetchMemberships(contactId?.let { Data.CONTACT_ID to it }) }
            .flowOn(Dispatchers.IO)

    fun observeRawContactGroupIds(rawContactId: Long): Flow<Set<Long>> =
        resolver.observe(Data.CONTENT_URI)
            .mapLatest { fetchMemberships(Data.RAW_CONTACT_ID to rawContactId).keys }
            .flowOn(Dispatchers.IO)

    fun observeAccounts(): Flow<List<ContactAccount>> =
        resolver.observe(RawContacts.CONTENT_URI)
            .mapLatest { (listOf(ContactAccount(null, null)) + fetchEditableAccounts()).distinct() }
            .flowOn(Dispatchers.IO)

    suspend fun getRawContactAccount(rawContactId: Long): ContactAccount? =
        withContext(Dispatchers.IO) {
            resolver.query(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                arrayOf(RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.toAccount(0) else null
            }
        }

    suspend fun createGroup(title: String, account: ContactAccount): Long? =
        withContext(Dispatchers.IO) {
            runCatching {
                resolver.insert(
                    Groups.CONTENT_URI,
                    contentValuesOf(
                        Groups.TITLE to title,
                        Groups.GROUP_VISIBLE to 1,
                        Groups.ACCOUNT_NAME to account.name,
                        Groups.ACCOUNT_TYPE to account.type
                    )
                )?.let(ContentUris::parseId)
            }.getOrNull()
        }

    suspend fun renameGroup(groupId: Long, title: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            resolver.update(
                ContentUris.withAppendedId(Groups.CONTENT_URI, groupId),
                contentValuesOf(Groups.TITLE to title),
                null,
                null
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun deleteGroup(groupId: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            resolver.delete(ContentUris.withAppendedId(Groups.CONTENT_URI, groupId), null, null) > 0
        }.getOrDefault(false)
    }

    suspend fun setMembership(
        rawContactId: Long,
        groupId: Long,
        isMember: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (isMember) {
                resolver.insert(
                    Data.CONTENT_URI,
                    contentValuesOf(
                        Data.RAW_CONTACT_ID to rawContactId,
                        Data.MIMETYPE to GroupMembership.CONTENT_ITEM_TYPE,
                        GroupMembership.GROUP_ROW_ID to groupId
                    )
                ) != null
            } else {
                resolver.delete(
                    Data.CONTENT_URI,
                    "${Data.MIMETYPE} = ? AND ${GroupMembership.GROUP_ROW_ID} = ? AND ${Data.RAW_CONTACT_ID} = ?",
                    arrayOf(GroupMembership.CONTENT_ITEM_TYPE, groupId.toString(), rawContactId.toString())
                ) > 0
            }
        }.getOrDefault(false)
    }

    private fun fetchGroups(): List<ContactGroup> = buildList {
        resolver.query(
            Groups.CONTENT_URI,
            arrayOf(
                Groups._ID,
                Groups.TITLE,
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.GROUP_IS_READ_ONLY,
                Groups.SYSTEM_ID
            ),
            "${Groups.DELETED} = 0 AND ${Groups.GROUP_VISIBLE} = 1 AND ${Groups.AUTO_ADD} = 0 AND ${Groups.FAVORITES} = 0",
            null,
            "${Groups.TITLE} COLLATE LOCALIZED ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(1)?.ifBlank { null } ?: continue
                add(
                    ContactGroup(
                        id = cursor.getLong(0),
                        title = title,
                        account = cursor.toAccount(2),
                        isReadOnly = cursor.getInt(4) == 1 || !cursor.isNull(5)
                    )
                )
            }
        }
    }

    private fun fetchMemberships(filter: Pair<String, Long>?): Map<Long, Set<Long>> {
        val memberships = mutableMapOf<Long, MutableSet<Long>>()
        val selection = buildString {
            append("${Data.MIMETYPE} = ?")
            if (filter != null) append(" AND ${filter.first} = ?")
        }

        resolver.query(
            Data.CONTENT_URI,
            arrayOf(GroupMembership.GROUP_ROW_ID, Data.CONTACT_ID),
            selection,
            listOfNotNull(GroupMembership.CONTENT_ITEM_TYPE, filter?.second?.toString()).toTypedArray(),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                memberships.getOrPut(cursor.getLong(0)) { mutableSetOf() }.add(cursor.getLong(1))
            }
        }

        return memberships
    }

    private fun fetchEditableAccounts(): List<ContactAccount> = buildList {
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE),
            "${RawContacts.DELETED} = 0 AND ${RawContacts.RAW_CONTACT_IS_READ_ONLY} = 0",
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) add(cursor.toAccount(0))
        }
    }

    private fun Cursor.toAccount(nameIndex: Int) = ContactAccount(
        name = getString(nameIndex)?.ifEmpty { null },
        type = getString(nameIndex + 1)?.ifEmpty { null }
    )
}
