package com.sosauce.cinnamon.presentation.screens.messages.components.bubble

import android.content.Intent
import android.provider.Telephony
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteAttachment
import com.sosauce.cinnamon.domain.model.CuteMessage
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.utils.getVcfName
import com.sosauce.cinnamon.utils.isAudio
import com.sosauce.cinnamon.utils.isImage
import com.sosauce.cinnamon.utils.isVcard
import com.sosauce.cinnamon.utils.isVideo
import ezvcard.Ezvcard

@Composable
fun MmsBubble(
    message: CuteMessage,
    sandwichPosition: SandwichPosition,
    bubbleColor: Color
) {
    val alignment = if (message.type == Telephony.Sms.MESSAGE_TYPE_INBOX)
        Alignment.Start else Alignment.End

    val attachment = message.attachment ?: return

    Column(horizontalAlignment = alignment) {
        CompositionLocalProvider(LocalContentColor provides contentColorFor(bubbleColor)) {
            attachment.attachmentDetails.fastForEach { details ->
                MmsAttachmentRouter(
                    details = details,
                    bubbleColor = bubbleColor,
                    sandwichPosition = sandwichPosition,
                    messageType = message.type
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
                isScheduled = message.isScheduled
            )
        }
    }
}

@Composable
private fun MmsAttachmentRouter(
    details: CuteAttachment.AttachmentDetails,
    bubbleColor: Color,
    sandwichPosition: SandwichPosition,
    messageType: Int
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val uri = details.uri


    when {
        uri.isImage(context) -> ImageAttachment(image = uri)

        uri.isVideo(context) -> VideoAttachment(video = uri)

        uri.isAudio(context) -> AudioAttachment(audio = uri, bubbleColor = bubbleColor)

        uri.isVcard(context) -> {
            val contactName by remember {
                derivedStateOf { uri.getVcfName(context) ?: resources.getString(R.string.unknown) }
            }

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
                            text = "View contact",
                            //text = Formatter.formatFileSize(context, details.size),
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
    messageType: Int
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
                    style = MaterialTheme.typography.bodyMedium
                )
                // Pro-tip: Move 'getMMSSize' to your Repository/ViewModel!
                // Don't calculate file size during composition.
                Text(
                    text = "TODO SIZE",
                    //text = Formatter.formatFileSize(context, details.size),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}