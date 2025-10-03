package com.sosauce.cuteconnect.data.conversation_settings

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sosauce.cuteconnect.domain.model.ConversationSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationSettingsDao {

    @Upsert
    suspend fun upsertConversation(conversationSettings: ConversationSettings)

    @Query("SELECT * FROM conversationsettings WHERE convoId = :convoInt LIMIT 1")
    fun getConversationSettings(convoInt: Long): Flow<ConversationSettings?>

}