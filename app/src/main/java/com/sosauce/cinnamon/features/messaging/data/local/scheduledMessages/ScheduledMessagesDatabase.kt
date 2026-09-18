package com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [ScheduledMessageEntity::class],
    version = 2
)
abstract class ScheduledMessagesDatabase : RoomDatabase() {
    abstract val dao: ScheduledMessagesDao
}

val MIGRATION_1_2_SCHEDULED_MESSAGE = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ScheduledMessage RENAME TO ScheduledMessageEntity")
    }
}

