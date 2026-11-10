package com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [ScheduledMessageEntity::class],
    version = 3
)
@androidx.room.TypeConverters(ScheduledMessageConverters::class)
abstract class ScheduledMessagesDatabase : RoomDatabase() {
    abstract val dao: ScheduledMessagesDao
}

class ScheduledMessageConverters {
    @androidx.room.TypeConverter
    fun fromAddresses(addresses: List<String>): String = addresses.joinToString("\n")

    @androidx.room.TypeConverter
    fun toAddresses(value: String): List<String> = value.split('\n').filter(String::isNotEmpty)
}

val MIGRATION_1_2_SCHEDULED_MESSAGE = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ScheduledMessage RENAME TO ScheduledMessageEntity")
    }
}

val MIGRATION_2_3_SCHEDULED_MESSAGE = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE ScheduledMessageEntity_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                threadId INTEGER NOT NULL,
                addresses TEXT NOT NULL,
                message TEXT NOT NULL,
                sendAt INTEGER NOT NULL
            )""".trimIndent()
        )
        connection.execSQL(
            """INSERT INTO ScheduledMessageEntity_new (id, threadId, addresses, message, sendAt)
                SELECT id, threadId, address, message, sendAt FROM ScheduledMessageEntity""".trimIndent()
        )
        connection.execSQL("DROP TABLE ScheduledMessageEntity")
        connection.execSQL("ALTER TABLE ScheduledMessageEntity_new RENAME TO ScheduledMessageEntity")
    }
}
