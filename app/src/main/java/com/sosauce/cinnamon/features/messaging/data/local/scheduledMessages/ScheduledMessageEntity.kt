package com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: Long = 0,
    val addresses: List<String> = emptyList(),
    val message: String = "",
    val sendAt: Long = 0
)
