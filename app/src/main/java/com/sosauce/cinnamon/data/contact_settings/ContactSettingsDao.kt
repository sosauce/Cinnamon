package com.sosauce.cinnamon.data.contact_settings

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactSettingsDao {

    @Upsert
    suspend fun upsertContact(contactSettings: ContactSettings)

    @Query("SELECT * FROM contactsettings WHERE contactId = :contactId LIMIT 1")
    fun getContactSettings(contactId: Long): Flow<ContactSettings?>

    @Query("SELECT poster FROM contactsettings WHERE contactId = :contactId LIMIT 1")
    fun getContactPoster(contactId: Long): String?

    @Query("DELETE FROM contactsettings WHERE contactId IN (:contactIds)")
    suspend fun deleteContactsSettings(contactIds: List<Long>)

    @Delete
    fun deleteContactSettings(contactSettings: ContactSettings)

}