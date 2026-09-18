@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.features.phone.data.repository

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.provider.VoicemailContract
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.utils.observe
import com.sosauce.cinnamon.features.phone.data.model.CuteVoicemailEntity
import com.sosauce.cinnamon.features.phone.data.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class VoicemailsRepository(
    private val context: Context,
    private val numberLookup: NumberLookup
) {

    fun fetchLatestVoicemails() =
        context.contentResolver.observe(VoicemailContract.Voicemails.CONTENT_URI).mapLatest {
            fetchVoicemails().fastMap { it.toDomain(context) }
        }.flowOn(Dispatchers.IO)


    private suspend fun fetchVoicemails(): List<CuteVoicemailEntity> = withContext(Dispatchers.IO) {

        val voicemails = mutableListOf<CuteVoicemailEntity>()

        val projection = arrayOf(
            VoicemailContract.Voicemails._ID,
            VoicemailContract.Voicemails.NUMBER,
            VoicemailContract.Voicemails.DURATION,
            VoicemailContract.Voicemails.DATE
            // VoicemailContract.Voicemails.HAS_CONTENT, // Do we need that to assume voicemail has audio ?
        )

        val selection = "${VoicemailContract.Voicemails.DELETED} != ?"
        val selectionArgs = arrayOf("1")

        context.contentResolver.query(
            VoicemailContract.Voicemails.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${VoicemailContract.Voicemails.DATE} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails._ID)
            val numberColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails.NUMBER)
            val durationColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails.DURATION)
            val dateColumn = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails.DATE)


            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val number = cursor.getString(numberColumn)
                val uri = ContentUris.withAppendedId(VoicemailContract.Voicemails.CONTENT_URI, id)
                val date = cursor.getLong(dateColumn)
                val duration = cursor.getLong(durationColumn)

                voicemails.add(
                    CuteVoicemailEntity(
                        id = id,
                        name = numberLookup.fetchContactDisplayName(number),
                        number = number,
                        date = date,
                        duration = duration,
                        photo = numberLookup.fetchPhoto(
                            number = number,
                            fullQuality = false
                        ),
                        voicemail = uri.toString()
                    )
                )

            }
        }

        return@withContext voicemails

    }

    suspend fun deleteVoicemails(ids: List<Long>) = withContext(Dispatchers.IO) {

        // This will set DELETED row to 1, not deleted off the DB
        try {
            val ops = ArrayList<ContentProviderOperation>()
            ids.fastForEach { id ->
                val uri = ContentUris.withAppendedId(
                    VoicemailContract.Voicemails.CONTENT_URI,
                    id
                )
                ops.add(ContentProviderOperation.newDelete(uri).build())
            }

            context.contentResolver.applyBatch(VoicemailContract.AUTHORITY, ops)

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}