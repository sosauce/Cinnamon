package com.sosauce.cinnamon.domain.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.CallLog
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteCallLog
import com.sosauce.cinnamon.utils.getContactId
import com.sosauce.cinnamon.utils.getContactPfpUriFromId
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale



class DialerPagingSource(
    private val dialerRepository: DialerRepository
) : PagingSource<Int, CuteCallLog>() {
    override fun getRefreshKey(state: PagingState<Int, CuteCallLog>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(20) ?: state.closestPageToPosition(anchor)?.nextKey?.minus(20)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CuteCallLog> {
        val pageIndex = params.key ?: 0
        val pageSize = params.loadSize

        return try {
            val logs = dialerRepository.fetchCallLogsPage(limit = pageSize, offset = pageIndex)
            LoadResult.Page(
                data = logs,
                prevKey = if (pageIndex == 0) null else pageIndex - pageSize,
                nextKey = if (logs.isEmpty()) null else pageIndex + pageSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}


class DialerRepository(
    private val context: Context
) {
    val callLogsObserver = context.contentResolver.observe(CallLog.Calls.CONTENT_URI)

    fun fetchCallLogsPagination(): Flow<PagingData<CuteCallLog>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { DialerPagingSource(this) }
        )
            .flow
            .flowOn(Dispatchers.IO)
    }

//    fun fetchLatestCallLog(limit: Int, offset: Int): Flow<List<CuteCallLog>> {
//        return context.contentResolver.observe(CallLog.Calls.CONTENT_URI).map {
//            fetchCallLogsPaginated(limit, offset)
//        }.flowOn(Dispatchers.IO)
//    }


    fun fetchCallLogsPage(limit: Int, offset: Int): List<CuteCallLog> {

        val logs = mutableListOf<CuteCallLog>()


        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE, // Incoming, outgoing, missed
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.NUMBER_PRESENTATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.GEOCODED_LOCATION
        )

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC LIMIT $limit OFFSET $offset"
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val callTypeColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val presentationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER_PRESENTATION)
            val cachedNameColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val cachedPictureColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
            val locationColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)
                val number = cursor.getString(numberColumn).ifEmpty { context.getString(R.string.private_number) }
                val callType = cursor.getInt(callTypeColumn)
                val date = cursor.getLong(dateColumn)
                val duration = cursor.getLong(durationColumn)
                val presentation = cursor.getInt(presentationColumn)
                val cachedName = cursor.getString(cachedNameColumn)?.ifEmpty { number }
                val cachedPicture = cursor.getString(cachedPictureColumn)?.toUri() ?: number.getContactId(context).getContactPfpUriFromId()
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
                        cachedPicture = cachedPicture
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