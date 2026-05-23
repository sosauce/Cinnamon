@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.data.datastore

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.edit
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.ARCHIVED_CONVOS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.DEFAULT_MESSAGES_SIM
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.DEFAULT_PHONE_HANDLE_ID
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.ENABLE_DELIVERY_REPORTS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.GROUP_SUBSEQUENT_CALLS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.PINNED_CONVOS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SEND_GROUP_AS_MMS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SEND_LONG_AS_MMS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_CONTACTS_ASCENDING
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_CONVERSATIONS_ASCENDING
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_LOGS_ASCENDING
import com.sosauce.cinnamon.utils.copyMutate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.json.Json

class UserPreferences(
    private val context: Context,
    private val telecomManager: TelecomManager
) {

    val pinnedConversations = context.dataStore.data.map {
        it[PINNED_CONVOS] ?: emptySet()
    }

    val archivedConversations = context.dataStore.data.map {
        it[ARCHIVED_CONVOS] ?: emptySet()
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


    val defaultMessagesSim = context.dataStore.data.map {
        it[DEFAULT_MESSAGES_SIM] ?: SubscriptionManager.getDefaultSmsSubscriptionId()
    }


    val sortLogsAscending = context.dataStore.data.map {
        it[SORT_LOGS_ASCENDING] ?: true
    }

    val groupSubsequentCalls = context.dataStore.data.map {
        it[GROUP_SUBSEQUENT_CALLS] ?: false
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
                    if (!remove(idString)) {
                        add(idString)
                    }
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
                    if (!remove(idString)) {
                        add(idString)
                    }
                }
            }

            it[PINNED_CONVOS] = newPinned
        }
    }

    fun getSortConversationsAscending() =
        context.dataStore.data.mapLatest { it[SORT_CONVERSATIONS_ASCENDING] ?: true }

    fun getSortContactsAscending() =
        context.dataStore.data.mapLatest { it[SORT_CONTACTS_ASCENDING] ?: true }


    @SuppressLint("MissingPermission")
    fun getDefaultPhoneHandle(): Flow<PhoneAccountHandle?> {
        return context.dataStore.data.map { preferences ->
            val savedId = preferences[DEFAULT_PHONE_HANDLE_ID] ?: ""

            if (savedId.isEmpty()) {
                println("Testing - Saved ID is empty")
                telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
            } else {
                println("Testing - Saved ID is equal to: $savedId")
                telecomManager.callCapablePhoneAccounts.fastFirstOrNull { handle ->
                    handle.id == savedId
                }
            }
        }
    }

    suspend fun saveDefaultPhoneHandleId(handle: PhoneAccountHandle) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PHONE_HANDLE_ID] = handle.id
            println("Testing - Saved ID: ${handle.id}")
        }
    }
}