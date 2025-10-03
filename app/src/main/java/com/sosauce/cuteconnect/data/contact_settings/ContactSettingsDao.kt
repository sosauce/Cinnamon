package com.sosauce.cuteconnect.data.contact_settings

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sosauce.cuteconnect.domain.model.ConversationSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactSettingsDao {

    @Upsert
    suspend fun upsertContact(contactSettings: ContactSettings)

    @Query("SELECT * FROM contactsettings WHERE id = :contactId LIMIT 1")
    fun getConversationSettings(contactId: Long): Flow<ContactSettings?>

}