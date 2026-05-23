@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.domain.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.CallLog
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteCallLog
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext


class DialerRepository(
    private val context: Context
) {

    fun fetchLatestCallLog(): Flow<List<CuteCallLog>> {
        return context.contentResolver.observe(CallLog.Calls.CONTENT_URI).mapLatest {
            fetchCallLogs()
        }.flowOn(Dispatchers.IO)
    }


    fun fetchCallLogs(): List<CuteCallLog> {

        val logs = mutableListOf<CuteCallLog>()


        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE, // Incoming, outgoing, missed
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.NUMBER_PRESENTATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.GEOCODED_LOCATION,
            CallLog.Calls.CACHED_PHOTO_ID
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

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)
                val number = cursor.getString(numberColumn)
                    .ifEmpty { context.getString(R.string.private_number) }
                val callType = cursor.getInt(callTypeColumn)
                val date = cursor.getLong(dateColumn)
                val duration = cursor.getLong(durationColumn)
                val presentation = cursor.getInt(presentationColumn)
                val cachedName = (cursor.getString(cachedNameColumn) ?: "").ifEmpty { number }
                val location = cursor.getString(locationColumn)

                logs.add(
                    CuteCallLog(
                        id = id,
                        rawNumber = number,
                        callType = callType,
                        date = date,
                        duration = duration,
                        location = location,
                        presentation = presentation,
                        cachedName = cachedName,
                        pfp = Uri.EMPTY
                        //pfp = number.getContactPfpFromNumber(context, false)
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