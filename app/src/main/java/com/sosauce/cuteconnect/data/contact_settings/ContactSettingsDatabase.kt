package com.sosauce.cuteconnect.data.contact_settings

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingsDao
import com.sosauce.cuteconnect.data.conversation_settings.RoomConverters
import com.sosauce.cuteconnect.domain.model.ConversationSettings

@Database(
    entities = [ContactSettings::class],
    version = 1
)
abstract class ContactSettingsDatabase : RoomDatabase() {
    abstract val dao: ContactSettingsDao
}