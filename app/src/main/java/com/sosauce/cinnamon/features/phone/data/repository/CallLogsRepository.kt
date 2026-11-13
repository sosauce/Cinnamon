@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.app.ActivityCompat
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.repository.SimsRepository
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.phone.data.model.CallLogsPage
import com.sosauce.cinnamon.features.phone.data.model.CuteCallLogEntity
import com.sosauce.cinnamon.features.phone.data.model.GroupedCallLog
import com.sosauce.cinnamon.features.phone.data.model.toDomain
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlin.text.ifEmpty


class CallLogsRepository(
    private val context: Context,
    private val numberLookup: NumberLookup,
    private val simsRepository: SimsRepository
) {

    fun fetchLatestCallLog(): Flow<List<CuteCallLog>> {
        return context.contentResolver.observe(CallLog.Calls.CONTENT_URI).mapLatest {
            fetchCallLogs().fastMap { it.toDomain(context) }
        }.flowOn(Dispatchers.IO)
    }


    suspend fun fetchCallLogs(): List<CuteCallLogEntity> {

        val logs = mutableListOf<CuteCallLogEntity>()

        // key = raw number, value = pair of display name to photo uri
        val lookupCache = mutableMapOf<String, Pair<String?, String?>>()


        val projection = CALL_LOG_PROJECTION

        val shouldBotherWithSims = simsRepository.hasMultipleSims()


        val sims = if (shouldBotherWithSims) {
            simsRepository.simLookup()
        } else emptyMap()

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->

            val columns = CallLogColumns(cursor)

            while (cursor.moveToNext()) {
                val callLog = readCallLog(cursor, columns, sims, lookupCache)

                logs.add(callLog)
            }
        }
        return logs
    }


    private class CallLogColumns(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
        val number = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val callType = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val date = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
        val duration = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
        val presentation = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER_PRESENTATION)
        val cachedName = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
        val location = cursor.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)
        val photo = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
        val subId = cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)
    }

    /**
     * Reads the call log at the cursor's current position
     *
     * @param lookupCache key = raw number, value = pair of display name to photo uri
     */
    private suspend fun readCallLog(
        cursor: Cursor,
        columns: CallLogColumns,
        sims: Map<Int, CuteSimCard>,
        lookupCache: MutableMap<String, Pair<String?, String?>>
    ): CuteCallLogEntity {
        val id = cursor.getLong(columns.id)
        val number = cursor.getString(columns.number).orEmpty()
        val callType = cursor.getInt(columns.callType)
        val date = cursor.getLong(columns.date)
        val duration = cursor.getLong(columns.duration)
        val presentation = cursor.getInt(columns.presentation)
        val cachedName = cursor.getString(columns.cachedName).orEmpty()
        val location = cursor.getString(columns.location)
        val photo = cursor.getString(columns.photo)
        val subId = cursor.getString(columns.subId)?.toIntOrNull() ?: 0

        val needsName = cachedName.isEmpty()
        val needsPhoto = photo.isNullOrEmpty()

        var resolvedName: String? = null
        var resolvedPhoto: String? = null
        val fetchContactInfo = (needsName || needsPhoto) && number.isNotEmpty()

        if (fetchContactInfo) {
            val cached = lookupCache.getOrPut(number) {
                numberLookup.fetchContactInfo(
                    number = number,
                    fullQuality = false
                )
            }

            val (lookupName, lookupPhoto) = cached

            if (needsName) resolvedName = lookupName
            if (needsPhoto) resolvedPhoto = lookupPhoto
        }
        val sim = sims[subId]

        return CuteCallLogEntity(
            id = id,
            number = number.ifEmpty { context.getString(R.string.private_number) },
            cachedName = cachedName.ifEmpty { resolvedName },
            date = date,
            duration = duration,
            location = location,
            presentation = presentation,
            type = callType,
            photo = if (!photo.isNullOrEmpty()) photo else resolvedPhoto,
            sim = sim
        )
    }


    fun observeCallLogChanges() = context.contentResolver.observe(CallLog.Calls.CONTENT_URI)

    /**
     * Loads up to [limit] call logs (or runs of subsequent calls with the same number when
     * [groupSubsequent] is enabled) matching [selection].
     *
     * @param fromRow cursor position to load from. Loads forward from it, or backward (rows before it)
     * when [backwards] is true. When [alignToGroup] is true, starts at the beginning of the run [fromRow] is in.
     */
    suspend fun getCallLogsPage(
        selection: String?,
        newestFirst: Boolean,
        groupSubsequent: Boolean,
        fromRow: Int,
        limit: Int,
        backwards: Boolean = false,
        alignToGroup: Boolean = false
    ): CallLogsPage = withContext(Dispatchers.IO) {
        val lookupCache = mutableMapOf<String, Pair<String?, String?>>()
        val sims = if (simsRepository.hasMultipleSims()) simsRepository.simLookup() else emptyMap()

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            CALL_LOG_PROJECTION,
            selection,
            null,
            "${CallLog.Calls.DATE} ${if (newestFirst) "DESC" else "ASC"}"
        )?.use { cursor ->
            val total = cursor.count
            if (total == 0 || limit <= 0) return@use CallLogsPage(emptyList(), 0, 0, total)

            val columns = CallLogColumns(cursor)
            fun numberAt(row: Int): String {
                cursor.moveToPosition(row)
                return cursor.getString(columns.number).orEmpty()
            }
            fun sameGroup(a: Int, b: Int) = groupSubsequent && numberAt(a) == numberAt(b)

            // row ranges of each group, in display order
            val groups = ArrayDeque<IntRange>()

            if (backwards) {
                var end = fromRow.coerceIn(0, total)
                while (groups.size < limit && end > 0) {
                    var start = end - 1
                    while (start > 0 && sameGroup(start - 1, start)) start--
                    groups.addFirst(start until end)
                    end = start
                }
            } else {
                var row = fromRow.coerceIn(0, total - 1)
                if (alignToGroup) {
                    while (row > 0 && sameGroup(row - 1, row)) row--
                }
                while (groups.size < limit && row < total) {
                    var end = row + 1
                    while (end < total && sameGroup(row, end)) end++
                    groups.addLast(row until end)
                    row = end
                }
            }

            val logs = groups.map { rows ->
                // a run is represented by its first call, like groupSubsequentlyBy()
                cursor.moveToPosition(rows.first)
                GroupedCallLog(
                    log = readCallLog(cursor, columns, sims, lookupCache).toDomain(context),
                    count = rows.last - rows.first + 1,
                    startRow = rows.first
                )
            }

            CallLogsPage(
                logs = logs,
                startRow = groups.firstOrNull()?.first ?: fromRow,
                endRow = groups.lastOrNull()?.let { it.last + 1 } ?: fromRow,
                totalRows = total
            )
        } ?: CallLogsPage(emptyList(), 0, 0, 0)
    }

    /**
     * Ids of the call logs whose name or number matches [query]
     */
    suspend fun searchCallLogIds(query: String): Set<Long> = withContext(Dispatchers.IO) {
        val like = "%${query.escapeLike()}%"
        val ids = mutableSetOf<Long>()

        // cached names & raw numbers
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID),
            "${CallLog.Calls.CACHED_NAME} LIKE ? ESCAPE '\\' OR ${CallLog.Calls.NUMBER} LIKE ? ESCAPE '\\'",
            arrayOf(like, like),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) ids.add(cursor.getLong(0))
        }

        // numbers of contacts whose name matches, for logs without a cached name
        val contactNumbers = mutableListOf<String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? ESCAPE '\\'",
            arrayOf(like),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) cursor.getString(0)?.let(contactNumbers::add)
        }

        // "06 12 34" should match "+33612345678"-like numbers, so compare digits only for number-like queries
        val queryDigits = query.filter(Char::isDigit)
        val isNumberQuery = queryDigits.length >= 3 && query.all { it.isDigit() || it in " +-()." }

        if (contactNumbers.isNotEmpty() || isNumberQuery) {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NORMALIZED_NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    if (id in ids) continue
                    val number = cursor.getString(1).orEmpty()
                    if (number.isEmpty()) continue

                    val matchesDigits = isNumberQuery && (
                            queryDigits in number.filter(Char::isDigit) ||
                                    queryDigits in cursor.getString(2).orEmpty().filter(Char::isDigit)
                            )

                    if (matchesDigits || contactNumbers.any { PhoneNumberUtils.compare(it, number) }) {
                        ids.add(id)
                    }
                }
            }
        }

        ids
    }

    private fun String.escapeLike(): String = this
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

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

    companion object {
        private val CALL_LOG_PROJECTION = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE, // Incoming, outgoing, missed
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.NUMBER_PRESENTATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.GEOCODED_LOCATION,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
    }
}
