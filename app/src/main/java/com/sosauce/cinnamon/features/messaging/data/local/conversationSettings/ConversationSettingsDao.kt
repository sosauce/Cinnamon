package com.sosauce.cinnamon.features.messaging.data.local.conversationSettings

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationSettingsDao {

    @Upsert
    suspend fun upsertConversation(conversationSettingsEntity: ConversationSettingsEntity)

    @Query("INSERT OR IGNORE INTO conversationsettingsentity (threadId, draft, wallpaper, wallpaperBlurIntensity, color) VALUES (:threadId, '', '', 0, -1)")
    suspend fun insertIfMissing(threadId: Long)

    @Query("UPDATE conversationsettingsentity SET draft = :draft WHERE threadId = :threadId")
    suspend fun setDraft(threadId: Long, draft: String)

    @Transaction
    suspend fun upsertDraft(threadId: Long, draft: String) {
        insertIfMissing(threadId)
        setDraft(threadId, draft)
    }

    @Query("SELECT * FROM conversationsettingsentity WHERE threadId = :threadId LIMIT 1")
    fun getConversationSettings(threadId: Long): Flow<ConversationSettingsEntity?>

    @Query("SELECT threadId, draft FROM conversationsettingsentity")
    fun getAllDrafts(): Flow<Map<@MapColumn(columnName = "threadId") Long, @MapColumn(columnName = "draft") String>>

    @Query("SELECT draft FROM conversationsettingsentity WHERE :threadId = threadId LIMIT 1")
    fun getDraftForThread(threadId: Long): Flow<String?>

    @Query("SELECT draft FROM conversationsettingsentity WHERE :threadId = threadId")
    fun getDraftForThreadOneShot(threadId: Long): String?

}