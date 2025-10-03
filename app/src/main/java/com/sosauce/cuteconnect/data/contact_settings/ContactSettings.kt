package com.sosauce.cuteconnect.data.contact_settings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ContactSettings(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 0,
    val poster: String = ""
)