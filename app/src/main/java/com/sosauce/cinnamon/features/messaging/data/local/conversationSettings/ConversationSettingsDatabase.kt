package com.sosauce.cinnamon.features.messaging.data.local.conversationSettings

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [ConversationSettingsEntity::class],
    version = 2,
)
abstract class ConversationSettingsDatabase : RoomDatabase() {
    abstract val dao: ConversationSettingsDao
}

val MIGRATION_1_2_CONVERSATION_SETTINGS = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ConversationSettings RENAME TO ConversationSettingsEntity")
    }
}
