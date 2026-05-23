package com.sosauce.cinnamon.data.schedulers.scheduled_messages

import android.provider.Telephony
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sosauce.cinnamon.domain.model.CuteMessage

@Entity
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: Long = 0,
    val address: String = "",
    val message: String = "",
    val sendAt: Long = 0
)

fun ScheduledMessage.toCuteMessage(): CuteMessage {
    return CuteMessage(
        id = id,
        body = message,
        address = address,
        date = sendAt,
        type = Telephony.Sms.MESSAGE_TYPE_SENT,
        isScheduled = true
    )
}

