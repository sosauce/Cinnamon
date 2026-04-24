@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.data.datastore

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.edit
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.ARCHIVED_CONVOS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.BLOCKED_NUMBERS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.DEFAULT_MESSAGES_SIM
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.ENABLE_DELIVERY_REPORTS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.PINNED_CONVOS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SEND_GROUP_AS_MMS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SEND_LONG_AS_MMS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_CONTACTS_ASCENDING
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_CONVERSATIONS_ASCENDING
import com.sosauce.cinnamon.utils.copyMutate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

class UserPreferences(private val context: Context) {

    val pinnedConversations = context.dataStore.data.map {
        it[PINNED_CONVOS] ?: emptySet()
    }

    val archivedConversations = context.dataStore.data.map {
        it[ARCHIVED_CONVOS] ?: emptySet()
    }

    val blockedNumbers = context.dataStore.data.map {
        it[BLOCKED_NUMBERS] ?: emptySet()
    }

    val groupAsMms = context.dataStore.data.map {
        it[SEND_GROUP_AS_MMS] ?: false
    }

    val longAsMms = context.dataStore.data.map {
        it[SEND_LONG_AS_MMS] ?: false
    }

    val enableDeliveryReports = context.dataStore.data.map {
        it[ENABLE_DELIVERY_REPORTS] ?: false
    }

    suspend fun saveBlockedNumbers(blockedNumbers: Set<String>) {
        context.dataStore.edit {
            it[BLOCKED_NUMBERS] = blockedNumbers
        }
    }

    val defaultMessagesSim = context.dataStore.data.map {
        it[DEFAULT_MESSAGES_SIM] ?: SubscriptionManager.getDefaultSmsSubscriptionId()
    }


    /**
     * Handles archiving or unarchiving
     */
    suspend fun toggleArchiveThreads(threadIds: List<Long>) {
        context.dataStore.edit {
            val alreadyArchived = it[ARCHIVED_CONVOS] ?: emptySet()



            val newArchived = alreadyArchived.copyMutate {
                threadIds.fastForEach { id ->
                    val idString = id.toString()
                    if (!remove(idString)) { add(idString) }
                }
            }

            it[ARCHIVED_CONVOS] = newArchived
        }
    }



    /**
     * Handles pinning or unpinning
     */

    suspend fun pinThreads(threadIds: List<Long>) {
        context.dataStore.edit {
            val alreadyPinned = it[PINNED_CONVOS] ?: emptySet()

            val newPinned = alreadyPinned.copyMutate {
                threadIds.fastForEach { id ->
                    val idString = id.toString()
                    if (!remove(idString)) { add(idString) }
                }
            }

            it[PINNED_CONVOS] = newPinned
        }
    }

    fun getSortConversationsAscending() = context.dataStore.data.mapLatest { it[SORT_CONVERSATIONS_ASCENDING] ?: true }
    fun getSortContactsAscending() = context.dataStore.data.mapLatest { it[SORT_CONTACTS_ASCENDING] ?: true }
}