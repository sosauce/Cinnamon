package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.utils.getVcfName
import com.sosauce.cinnamon.core.utils.isVcard
import com.sosauce.cinnamon.features.messaging.data.model.AttachmentType
import com.sosauce.cinnamon.features.messaging.data.model.CuteAttachment
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.domain.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MmsBubble(
    message: CuteMessage,
    sandwichPosition: SandwichPosition,
    bubbleColor: Color,
    onHandleConversationActions: (com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions) -> Unit,
    highlightQuery: String? = null,
    isFocusedMatch: Boolean = false
) {
    val alignment = if (message.type == MessageType.RECEIVED)
        Alignment.Start else Alignment.End

    val attachment = message.attachment ?: return

    Column(horizontalAlignment = alignment) {
        CompositionLocalProvider(LocalContentColor provides contentColorFor(bubbleColor)) {
            attachment.attachmentDetails.fastForEach { details ->
                MmsAttachmentRouter(
                    details = details,
                    bubbleColor = bubbleColor,
                    sandwichPosition = sandwichPosition,
                    messageType = message.type,
                    onHandleConversationActions = onHandleConversationActions
                )
            }
        }

        if (attachment.body.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            TextBubbleContent(
                text = attachment.body,
                type = message.type,
                bubbleColor = bubbleColor,
                sandwichPosition = sandwichPosition,
                isScheduled = message.isScheduled,
                highlightQuery = highlightQuery,
                isFocusedMatch = isFocusedMatch
            )
        }
    }
}

@Composable
private fun MmsAttachmentRouter(
    details: CuteAttachment.AttachmentDetails,
    bubbleColor: Color,
    sandwichPosition: SandwichPosition,
    messageType: MessageType,
    onHandleConversationActions: (com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val uri = details.uri
    val isMedia = details.attachmentType == AttachmentType.IMAGE ||
            details.attachmentType == AttachmentType.VIDEO ||
            details.attachmentType == AttachmentType.AUDIO

    // vCard name, or null if not a vCard (or not resolved yet).
    val vcardName by produceState<String?>(initialValue = null, uri, isMedia) {
        if (isMedia) return@produceState
        value = withContext(Dispatchers.IO) {
            if (uri.isVcard(context)) {
                uri.getVcfName(context) ?: resources.getString(R.string.unknown)
            } else null
        }
    }

    when {
        details.attachmentType == AttachmentType.IMAGE -> ImageAttachment(
            image = uri,
            onHandleConversationActions = onHandleConversationActions
        )

        details.attachmentType == AttachmentType.VIDEO -> VideoAttachment(video = uri)

        details.attachmentType == AttachmentType.AUDIO -> AudioAttachment(
            audio = uri,
            bubbleColor = bubbleColor
        )

        vcardName != null -> {
            val contactName = vcardName.orEmpty()

            Box(
                modifier = Modifier
                    .clip(BubbleShape(sandwichPosition, messageType))
                    .background(bubbleColor)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                details.uri,
                                context.contentResolver.getType(details.uri)
                            )
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(all = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultContactIcon(
                        firstLetter = contactName.firstOrNull()
                    )
                    Column {
                        Text(
                            text = contactName,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                            style = MaterialTheme.typography.bodyMediumEmphasized
                        )
                        Text(
                            text = Formatter.formatFileSize(context, details.size),
                            style = MaterialTheme.typography.labelSmallEmphasized
                        )
                    }
                }
            }
        }

        else -> FileAttachment(
            details = details,
            bubbleColor = bubbleColor,
            sandwichPosition = sandwichPosition,
            messageType = messageType
        )
    }
}

@Composable
private fun FileAttachment(
    details: CuteAttachment.AttachmentDetails,
    bubbleColor: Color,
    sandwichPosition: SandwichPosition,
    messageType: MessageType
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .clip(BubbleShape(sandwichPosition, messageType))
            .background(bubbleColor)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(details.uri, context.contentResolver.getType(details.uri))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }
    ) {
        Row(
            modifier = Modifier.padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(painter = painterResource(R.drawable.file), contentDescription = null)
            Column {
                Text(
                    text = details.filename,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    style = MaterialTheme.typography.bodyMediumEmphasized
                )
                Text(
                    text = Formatter.formatFileSize(context, details.size),
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
        }
    }
}