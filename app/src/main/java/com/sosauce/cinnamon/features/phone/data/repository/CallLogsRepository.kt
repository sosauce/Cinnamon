@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
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
import com.sosauce.cinnamon.features.phone.data.model.CuteCallLogEntity
import com.sosauce.cinnamon.features.phone.data.model.toDomain
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
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


        val projection = arrayOf(
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

            val idColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val callTypeColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val presentationColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER_PRESENTATION)
            val cachedNameColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val locationColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)
            val photoColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
            val subIdColumnIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)

            while (cursor.moveToNext()) {


                val id = cursor.getLong(idColumnIndex)
                val number = cursor.getString(numberColumnIndex).orEmpty()
                val callType = cursor.getInt(callTypeColumnIndex)
                val date = cursor.getLong(dateColumnIndex)
                val duration = cursor.getLong(durationColumnIndex)
                val presentation = cursor.getInt(presentationColumnIndex)
                val cachedName = cursor.getString(cachedNameColumnIndex).orEmpty()
                val location = cursor.getString(locationColumnIndex)
                val photo = cursor.getString(photoColumnIndex)
                val subId = cursor.getString(subIdColumnIndex)?.toIntOrNull() ?: 0

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

                val callLog = CuteCallLogEntity(
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

                logs.add(callLog)
            }
        }
        return logs
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
