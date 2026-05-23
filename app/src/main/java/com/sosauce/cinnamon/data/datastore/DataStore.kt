package com.sosauce.cinnamon.data.datastore

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.ARCHIVED_CONVOS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.DEFAULT_MESSAGES_SIM
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.DEFAULT_TAB
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.ENABLE_DELIVERY_REPORTS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.ENABLE_T9_DIALING
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.GROUP_SUBSEQUENT_CALLS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.MMS_MAX_SIZE_LIMIT
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.PALETTE_STYLE
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.PINNED_CONVOS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SEND_GROUP_AS_MMS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SEND_LONG_AS_MMS
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SHOW_CHAR_COUNT
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_CONTACTS_ASCENDING
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_CONVERSATIONS_ASCENDING
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.SORT_LOGS_ASCENDING
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.THEME
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.USE_SYSTEM_FONT
import com.sosauce.cinnamon.utils.CutePaletteStyle
import com.sosauce.cinnamon.utils.CuteTheme
import com.sosauce.cinnamon.utils.DefaultTabOption
import com.sosauce.cinnamon.utils.MmsSize

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
    val DEFAULT_TAB = stringPreferencesKey("DEFAULT_TAB")

    val GROUP_SUBSEQUENT_CALLS = booleanPreferencesKey("GROUP_SUBSEQUENT_CALLS")
    val SORT_CONVERSATIONS_ASCENDING = booleanPreferencesKey("SORT_CONVERSATIONS_ASCENDING")
    val SORT_CONTACTS_ASCENDING = booleanPreferencesKey("SORT_CONTACTS_ASCENDING")
    val ENABLE_T9_DIALING = booleanPreferencesKey("ENABLE_T9_DIALING")
    val SORT_LOGS_ASCENDING = booleanPreferencesKey("SORT_LOGS_ASCENDING")
}


@Composable
fun rememberAppTheme() =
    rememberPreference(key = THEME, defaultValue = CuteTheme.SYSTEM)

@Composable
fun rememberUseSystemFont() =
    rememberPreference(key = USE_SYSTEM_FONT, defaultValue = false)

@Composable
fun rememberPaletteStyle() =
    rememberPreference(key = PALETTE_STYLE, defaultValue = CutePaletteStyle.TONAL_SPOT)

@Composable
fun rememberPinnedConversations() =
    rememberPreference(key = PINNED_CONVOS, defaultValue = emptySet())

@Composable
fun rememberArchivedConversations() =
    rememberPreference(key = ARCHIVED_CONVOS, defaultValue = emptySet())

@Composable
fun rememberMmsMaxSizeLimit() = rememberPreference(MMS_MAX_SIZE_LIMIT, MmsSize.FILE_SIZE_600_KB)


@Composable
fun rememberDefaultMessagesSim() =
    rememberPreference(DEFAULT_MESSAGES_SIM, SubscriptionManager.getDefaultSmsSubscriptionId())

@Composable
fun rememberShowCharCount() = rememberPreference(SHOW_CHAR_COUNT, false)

@Composable
fun rememberSendGroupAsMms() = rememberPreference(SEND_GROUP_AS_MMS, false)

@Composable
fun rememberEnableDeliveryReports() = rememberPreference(ENABLE_DELIVERY_REPORTS, false)

@Composable
fun rememberSendLongAsMms() = rememberPreference(SEND_LONG_AS_MMS, false)

@Composable
fun rememberDefaultTab() = rememberPreference(DEFAULT_TAB, DefaultTabOption.MESSAGES)

@Composable
fun rememberGroupSubsequentCalls() = rememberPreference(GROUP_SUBSEQUENT_CALLS, false)

@Composable
fun rememberSortConversationsAscending() = rememberPreference(SORT_CONVERSATIONS_ASCENDING, true)

@Composable
fun rememberSortContactsAscending() = rememberPreference(SORT_CONTACTS_ASCENDING, true)

@Composable
fun rememberEnableT9Dialing() = rememberPreference(ENABLE_T9_DIALING, true)

@Composable
fun rememberSortLogsAscending() = rememberPreference(SORT_LOGS_ASCENDING, true)
