@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components.bottombar

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.ImageAttachment
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.VideoAttachment
import com.sosauce.cinnamon.utils.getFileName
import com.sosauce.cinnamon.utils.getMMSSize
import com.sosauce.cinnamon.utils.isImage
import com.sosauce.cinnamon.utils.isVideo

@Composable
fun ReadyToSendAttachment(
    modifier: Modifier = Modifier,
    attachment: Uri,
    onRemoveAttachment: () -> Unit
) {

    val context = LocalContext.current

    Box(modifier = modifier) {
        when {
            attachment.isImage(context) -> {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) { ImageAttachment(image = attachment, onHandleConversationActions = {}) }
            }

            attachment.isVideo(context) -> {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) { VideoAttachment(video = attachment) }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(70.dp)
                        .width(150.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.file_filled),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text(
                            text = attachment.getFileName(context) ?: "",
                            modifier = Modifier.basicMarquee(),
                            style = MaterialTheme.typography.bodySmallEmphasized
                        )
                        Text(
                            text = Formatter.formatFileSize(
                                context,
                                context.getMMSSize(attachment)
                            ),
                            style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        FilledIconButton(
            onClick = onRemoveAttachment,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(4.dp, (-7).dp)
                .size(IconButtonDefaults.extraSmallIconSize)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize)
            )
        }
    }
}