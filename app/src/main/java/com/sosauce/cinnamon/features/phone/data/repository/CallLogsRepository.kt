@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.phone.data.model.CuteCallLogEntity
import com.sosauce.cinnamon.features.phone.data.model.toDomain
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext


class CallLogsRepository(
    private val context: Context,
    private val numberLookup: NumberLookup
) {

    fun fetchLatestCallLog(): Flow<List<CuteCallLog>> {
        return context.contentResolver.observe(CallLog.Calls.CONTENT_URI).mapLatest {
            fetchCallLogs().fastMap { it.toDomain(context) }
        }.flowOn(Dispatchers.IO)
    }


    suspend fun fetchCallLogs(): List<CuteCallLogEntity> {

        val logs = mutableListOf<CuteCallLogEntity>()
        val lookupCache = mutableMapOf<String, Pair<String?, String?>>()

        queryCallLogs(newestFirst = true)?.use { cursor ->
            while (cursor.moveToNext()) {
                logs.add(cursor.readCallLog(lookupCache))
            }
        }
        return logs
    }

    private fun queryCallLogs(newestFirst: Boolean): Cursor? {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE, // Incoming, outgoing, missed
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.NUMBER_PRESENTATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.GEOCODED_LOCATION,
            CallLog.Calls.CACHED_PHOTO_URI
        )

        return context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} ${if (newestFirst) "DESC" else "ASC"}"
        )
    }

    // Memo of PhoneLookup results keyed by the raw number string.
    // Same number across rows is queried once, negative (null) results
    // are cached too so unknown numbers don't re-query per row.
    private suspend fun Cursor.readCallLog(
        lookupCache: MutableMap<String, Pair<String?, String?>>
    ): CuteCallLogEntity {
        val id = getLong(getColumnIndexOrThrow(CallLog.Calls._ID))
        val number = getString(getColumnIndexOrThrow(CallLog.Calls.NUMBER))
        val callType = getInt(getColumnIndexOrThrow(CallLog.Calls.TYPE))
        val date = getLong(getColumnIndexOrThrow(CallLog.Calls.DATE))
        val duration = getLong(getColumnIndexOrThrow(CallLog.Calls.DURATION))
        val presentation = getInt(getColumnIndexOrThrow(CallLog.Calls.NUMBER_PRESENTATION))
        val cachedName = getString(getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)) ?: ""
        val location = getString(getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION))
        val photo = getString(getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI))

        val needsName = cachedName.isEmpty()
        val needsPhoto = photo.isNullOrEmpty()

        var resolvedName: String? = null
        var resolvedPhoto: String? = null
        if ((needsName || needsPhoto) && number.isNotEmpty()) {
            val cached = lookupCache.getOrPut(number) {
                numberLookup.fetchContactInfo(
                    number = number,
                    fullQuality = false
                )
            }
            if (needsName) resolvedName = cached.first
            if (needsPhoto) resolvedPhoto = cached.second
        }

        return CuteCallLogEntity(
            id = id,
            number = number.ifEmpty { context.getString(R.string.private_number) },
            cachedName = cachedName.ifEmpty { resolvedName },
            date = date,
            duration = duration,
            location = location,
            presentation = presentation,
            type = callType,
            photo = if (!photo.isNullOrEmpty()) photo else resolvedPhoto
        )
    }

    suspend fun deleteCallLog(ids: List<Long>) = withContext(Dispatchers.IO) {


        val ops = ArrayList<ContentProviderOperation>()

        ids.fastForEach { id ->
            ops.add(
                ContentProviderOperation
                    .newDelete(CallLog.Calls.CONTENT_URI)
                    .withSelection("${CallLog.Calls._ID} = ?", arrayOf(id.toString()))
                    .build()
            )
        }

        context.contentResolver.applyBatch(CallLog.AUTHORITY, ops)
    }

}
