package com.sosauce.cinnamon.domain.model

import android.provider.Telephony.Sms
import com.sosauce.cinnamon.data.broadcasts.DeliveryReportsReceiver

/**
 * Define what a message is.
 * Can be an SMS or MMS message.
 *
 * @param id Unique id of message.
 * @param body The content of the message.
 * @param type Type of messages, such a received, sent etc...
 * @param address The number of the other party.
 * @param date Date when the message was sent/received.
 * @param threadId Unique thread id to which this message belongs to. Use this to navigate to a conversation screen.
 * @param read Is the message read ?
 * @param isMms Is it an MMS ? Note that group messages / RCS messages are treated as MMS
 * @param isScheduled Whether this message is scheduled or not
 * @param delivered Whether a message has been marked delivered by the carrier using [DeliveryReportsReceiver]
 */
data class CuteMessage(
    val id: Long = 0,
    val body: String = "",
    val type: Int = Sms.MESSAGE_TYPE_INBOX,
    val address: String = "",
    val date: Long = 0,
    val threadId: Long = 0,
    val read: Boolean = true,
    val isMms: Boolean = false,
    val attachment: CuteAttachment? = null,
    val isScheduled: Boolean = false,
    val delivered: Boolean = false
)
