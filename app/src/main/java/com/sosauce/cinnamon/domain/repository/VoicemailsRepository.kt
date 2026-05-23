@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sosauce.cinnamon.domain.repository

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.provider.VoicemailContract
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.domain.model.CuteVoicemail
import com.sosauce.cinnamon.utils.getContactNameOrNothing
import com.sosauce.cinnamon.utils.observe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class VoicemailsRepository(
    private val context: Context
) {

    fun fetchLatestVoicemails() =
        context.contentResolver.observe(VoicemailContract.Voicemails.CONTENT_URI).mapLatest {
            fetchVoicemails()
        }.flowOn(Dispatchers.IO)


    private suspend fun fetchVoicemails(): List<CuteVoicemail> = withContext(Dispatchers.IO) {

        val voicemails = mutableListOf<CuteVoicemail>()

        val projection = arrayOf(
            VoicemailContract.Voicemails._ID,
            VoicemailContract.Voicemails.NUMBER,
            VoicemailContract.Voicemails.DURATION,
            VoicemailContract.Voicemails.DATE,
            VoicemailContract.Voicemails.DELETED,
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
            val a = cursor.getColumnIndexOrThrow(VoicemailContract.Voicemails.DELETED)


            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val number = cursor.getString(numberColumn)
                val uri = ContentUris.withAppendedId(VoicemailContract.Voicemails.CONTENT_URI, id)
                val date = cursor.getLong(dateColumn)
                val duration = cursor.getLong(durationColumn)
                val displayName = number.getContactNameOrNothing(context)
                val deleted = cursor.getInt(a)

                println("CACA: $deleted")


                voicemails.add(
                    CuteVoicemail(
                        id = id,
                        number = number,
                        displayName = displayName,
                        uri = uri,
                        duration = duration,
                        date = date
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
            println("Delete voicemails: ${e.message}")
        }

    }

}