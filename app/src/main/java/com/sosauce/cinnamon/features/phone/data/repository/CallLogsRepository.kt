@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.CallLog
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.phone.data.model.CuteCallLogEntity
import com.sosauce.cinnamon.features.phone.data.model.toDomain
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
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

    fun fetchLatestCallLog(): Flow<List<CuteCallLog2>> {
        return context.contentResolver.observe(CallLog.Calls.CONTENT_URI).mapLatest {
            fetchCallLogs().fastMap { it.toDomain(context) }
        }.flowOn(Dispatchers.IO)
    }


    suspend fun fetchCallLogs(): List<CuteCallLogEntity> {

        val logs = mutableListOf<CuteCallLogEntity>()


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


        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val callTypeColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val presentationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER_PRESENTATION)
            val cachedNameColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val locationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)
            val photoColumn =  cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)
                val number = cursor.getString(numberColumn)
                val callType = cursor.getInt(callTypeColumn)
                val date = cursor.getLong(dateColumn)
                val duration = cursor.getLong(durationColumn)
                val presentation = cursor.getInt(presentationColumn)
                val cachedName = cursor.getString(cachedNameColumn)
                val location = cursor.getString(locationColumn)
                val photo = cursor.getString(photoColumn)

                logs.add(
                    CuteCallLogEntity(
                        id = id,
                        number = number.ifEmpty { context.getString(R.string.private_number) },
                        cachedName = cachedName,
                        date = date,
                        duration = duration,
                        location = location,
                        presentation = presentation,
                        type = callType,
                        photo = photo?.ifEmpty {
                            numberLookup.fetchPhoto(
                                number = number,
                                fullQuality = false
                            )
                        } ?: numberLookup.fetchPhoto(
                            number = number,
                            fullQuality = false
                        )
                    )
                )

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