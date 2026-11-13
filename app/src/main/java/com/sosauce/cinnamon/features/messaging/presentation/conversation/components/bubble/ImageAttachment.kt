@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.about.ImagePage
import com.sosauce.nekobites.animations.AnimatedFab
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun ImageAttachment(
    modifier: Modifier = Modifier,
    image: Uri?,
    onHandleConversationActions: (ConversationActions) -> Unit,
) {

    var showFullscreen by remember { mutableStateOf(false) }

    if (showFullscreen) {

        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.cloudy(250)
                ) {
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ImagePage(image ?: Uri.EMPTY)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedFab(
                        onClick = { showFullscreen = false },
                        icon = R.drawable.keyboard_down,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                    AnimatedFab(
                        onClick = {
                            onHandleConversationActions(
                                ConversationActions.DownloadMmsImage(
                                    image ?: return@AnimatedFab
                                )
                            )
                        },
                        icon = R.drawable.download
                    )
                }
            }
        }
    }
    AsyncImage(
        model = image,
        contentDescription = null,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { showFullscreen = true },
        contentScale = ContentScale.Crop
    )


}