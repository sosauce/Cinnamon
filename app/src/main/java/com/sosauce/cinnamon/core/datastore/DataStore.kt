package com.sosauce.cinnamon.core.datastore

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sosauce.cinnamon.core.datastore.PreferencesKeys.DEFAULT_TAB
import com.sosauce.cinnamon.core.utils.CutePaletteStyle
import com.sosauce.cinnamon.core.utils.CuteTheme
import com.sosauce.cinnamon.core.utils.DefaultTabOption
import com.sosauce.cinnamon.core.utils.MmsSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private const val PREFERENCES_NAME = "settings"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(PREFERENCES_NAME)

data object PreferencesKeys {
    val THEME = stringPreferencesKey("theme")
    val USE_SYSTEM_FONT = booleanPreferencesKey("use_sys_font")
    val PALETTE_STYLE = stringPreferencesKey("PALETTE_STYLE")

    val PINNED_CONVOS = stringSetPreferencesKey("pinned_convos")
    val ARCHIVED_CONVOS = stringSetPreferencesKey("archived_convos")
    val MMS_MAX_SIZE_LIMIT = longPreferencesKey("MMS_MAX_SIZE_LIMIT")
    val DEFAULT_MESSAGES_SIM = intPreferencesKey("DEFAULT_MESSAGES_SIM")
    val DEFAULT_PHONE_HANDLE_ID = stringPreferencesKey("DEFAULT_PHONE_HANDLE_ID")
    val SEND_LONG_AS_MMS = booleanPreferencesKey("SEND_LONG_AS_MMS")
    val SHOW_CHAR_COUNT = booleanPreferencesKey("DISPLAY_CHAR_COUNT")
    val SEND_GROUP_AS_MMS = booleanPreferencesKey("SEND_GROUP_AS_MMS")
    val ENABLE_DELIVERY_REPORTS = booleanPreferencesKey("ENABLE_DELIVERY_REPORTS")
    val DEFAULT_TAB = intPreferencesKey("DEFAULT_TAB")

    val GROUP_SUBSEQUENT_CALLS = booleanPreferencesKey("GROUP_SUBSEQUENT_CALLS")
    val SORT_CONVERSATIONS_ASCENDING = booleanPreferencesKey("SORT_CONVERSATIONS_ASCENDING")
    val SORT_CONTACTS_ASCENDING = booleanPreferencesKey("SORT_CONTACTS_ASCENDING")
    val ENABLE_T9_DIALING = booleanPreferencesKey("ENABLE_T9_DIALING")
    val SORT_LOGS_ASCENDING = booleanPreferencesKey("SORT_LOGS_ASCENDING")
    val INCOMING_CALL_FULLSCREEN = booleanPreferencesKey("INCOMING_CALL_FULLSCREEN")
}


@Composable
fun rememberAppTheme() =
    rememberPreference(key = PreferencesKeys.THEME, defaultValue = CuteTheme.SYSTEM)

@Composable
fun rememberUseSystemFont() =
    rememberPreference(key = PreferencesKeys.USE_SYSTEM_FONT, defaultValue = false)

@Composable
fun rememberPaletteStyle() =
    rememberPreference(key = PreferencesKeys.PALETTE_STYLE, defaultValue = CutePaletteStyle.TONAL_SPOT)

@Composable
fun rememberPinnedConversations() =
    rememberPreference(key = PreferencesKeys.PINNED_CONVOS, defaultValue = emptySet())

@Composable
fun rememberArchivedConversations() =
    rememberPreference(key = PreferencesKeys.ARCHIVED_CONVOS, defaultValue = emptySet())

@Composable
fun rememberMmsMaxSizeLimit() = rememberPreference(PreferencesKeys.MMS_MAX_SIZE_LIMIT, MmsSize.FILE_SIZE_600_KB)


@Composable
fun rememberDefaultMessagesSim() =
    rememberPreference(PreferencesKeys.DEFAULT_MESSAGES_SIM, SubscriptionManager.getDefaultSmsSubscriptionId())

@Composable
fun rememberShowCharCount() = rememberPreference(PreferencesKeys.SHOW_CHAR_COUNT, false)

@Composable
fun rememberSendGroupAsMms() = rememberPreference(PreferencesKeys.SEND_GROUP_AS_MMS, false)

@Composable
fun rememberEnableDeliveryReports() = rememberPreference(PreferencesKeys.ENABLE_DELIVERY_REPORTS, false)

@Composable
fun rememberSendLongAsMms() = rememberPreference(PreferencesKeys.SEND_LONG_AS_MMS, false)


@Composable
fun rememberGroupSubsequentCalls() = rememberPreference(PreferencesKeys.GROUP_SUBSEQUENT_CALLS, false)

@Composable
fun rememberSortConversationsAscending() = rememberPreference(PreferencesKeys.SORT_CONVERSATIONS_ASCENDING, true)

@Composable
fun rememberSortContactsAscending() = rememberPreference(PreferencesKeys.SORT_CONTACTS_ASCENDING, true)

@Composable
fun rememberEnableT9Dialing() = rememberPreference(PreferencesKeys.ENABLE_T9_DIALING, true)

@Composable
fun rememberSortLogsAscending() = rememberPreference(PreferencesKeys.SORT_LOGS_ASCENDING, true)

@Composable
fun rememberIncomingCallFullscreen() = rememberPreference(PreferencesKeys.INCOMING_CALL_FULLSCREEN, true)

@Composable
fun rememberInitialScreen() = rememberPreference(PreferencesKeys.DEFAULT_TAB, DefaultTabOption.MESSAGES)

@Composable
fun rememberInitialScreenBlocking(): Int {
    val context = LocalContext.current

    return runBlocking {
        context.dataStore.data.map { it[DEFAULT_TAB] ?: DefaultTabOption.MESSAGES }.first()
    }
}
