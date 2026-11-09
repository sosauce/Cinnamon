package com.sosauce.cinnamon.features.messaging.data.local.conversationSettings

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [ConversationSettingsEntity::class],
    version = 3,
)
abstract class ConversationSettingsDatabase : RoomDatabase() {
    abstract val dao: ConversationSettingsDao
}

val MIGRATION_1_2_CONVERSATION_SETTINGS = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ConversationSettings RENAME TO ConversationSettingsEntity")
    }
}

val MIGRATION_2_3_CONVERSATION_SETTINGS = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ConversationSettingsEntity_new` (
                `threadId` INTEGER NOT NULL,
                `draft` TEXT NOT NULL,
                `wallpaper` TEXT NOT NULL,
                `wallpaperBlurIntensity` INTEGER NOT NULL,
                `color` INTEGER NOT NULL,
                PRIMARY KEY(`threadId`)
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO `ConversationSettingsEntity_new` (threadId, draft, wallpaper, wallpaperBlurIntensity, color)
            SELECT t.threadId,
                COALESCE((SELECT draft FROM ConversationSettingsEntity o WHERE o.threadId = t.threadId ORDER BY o.id DESC LIMIT 1), ''),
                COALESCE((SELECT wallpaper FROM ConversationSettingsEntity o WHERE o.threadId = t.threadId AND o.wallpaper NOT IN ('', 'null') ORDER BY o.id DESC LIMIT 1), ''),
                COALESCE((SELECT wallpaperBlurIntensity FROM ConversationSettingsEntity o WHERE o.threadId = t.threadId AND o.wallpaperBlurIntensity > 0 ORDER BY o.id DESC LIMIT 1), 0),
                COALESCE((SELECT color FROM ConversationSettingsEntity o WHERE o.threadId = t.threadId AND o.color != -1 ORDER BY o.id DESC LIMIT 1), -1)
            FROM (SELECT DISTINCT threadId FROM ConversationSettingsEntity) t
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE ConversationSettingsEntity")
        connection.execSQL("ALTER TABLE `ConversationSettingsEntity_new` RENAME TO `ConversationSettingsEntity`")
    }
}
