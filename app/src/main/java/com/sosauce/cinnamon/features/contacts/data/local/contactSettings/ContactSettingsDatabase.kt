package com.sosauce.cinnamon.features.contacts.data.local.contactSettings

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [ContactSettingsEntity::class],
    version = 2
)
abstract class ContactSettingsDatabase : RoomDatabase() {
    abstract val dao: ContactSettingsDao
}

val MIGRATION_1_2_CONTACT_SETTINGS = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ContactSettings RENAME TO ContactSettingsEntity")
    }
}

