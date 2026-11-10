@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.getFileName
import com.sosauce.cinnamon.core.utils.getMMSSize
import com.sosauce.cinnamon.core.utils.isImage
import com.sosauce.cinnamon.core.utils.isVideo
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.ImageAttachment
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.VideoAttachment

private val AttachmentTileHeight = 96.dp

@Composable
fun ReadyToSendAttachment(
    modifier: Modifier = Modifier,
    attachment: Uri,
    shape: Shape,
    onRemoveAttachment: () -> Unit
) {

    val context = LocalContext.current
    val isImage = remember(attachment) { attachment.isImage(context) }
    val isVideo = remember(attachment) { attachment.isVideo(context) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        when {
            isImage -> {
                ImageAttachment(
                    image = attachment,
                    onHandleConversationActions = {},
                    modifier = Modifier.size(AttachmentTileHeight)
                )
            }

            isVideo -> {
                VideoAttachment(
                    video = attachment,
                    modifier = Modifier.size(AttachmentTileHeight)
                )
            }

            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .size(width = 184.dp, height = AttachmentTileHeight)
                        .padding(start = 12.dp, end = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.file_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = attachment.getFileName(context) ?: "",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = Formatter.formatFileSize(
                                context,
                                context.getMMSSize(attachment)
                            ),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        FilledIconButton(
            onClick = onRemoveAttachment,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.remove_attachment),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun animatedAttachmentShape(
    isFirst: Boolean,
    isLast: Boolean
): Shape {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val start by animateDpAsState(if (isFirst) 24.dp else 2.dp, spec)
    val end by animateDpAsState(if (isLast) 24.dp else 2.dp, spec)

    return RoundedCornerShape(
        topStart = start,
        bottomStart = start,
        topEnd = end,
        bottomEnd = end
    )
}
