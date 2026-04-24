@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components.bottombar

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import com.sosauce.cinnamon.utils.isImage
import com.sosauce.cinnamon.utils.isVideo

@Composable
fun ReadyToSendAttachment(
    modifier: Modifier = Modifier,
    attachment: Uri,
    onRemoveAttachment: () -> Unit
) {

    val context = LocalContext.current

    Box {
        Box(
            modifier = modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            when {
                attachment.isImage(context) -> ImageAttachment(image = attachment)
                attachment.isVideo(context) -> VideoAttachment(video = attachment)
                else -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.file_filled),
                            contentDescription = null
                        )
                        Text(
                            text = attachment.getFileName(context) ?: "",
                            modifier = Modifier.basicMarquee(),
                            style = MaterialTheme.typography.bodySmallEmphasized
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