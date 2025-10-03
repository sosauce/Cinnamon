@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.messages.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sosauce.cuteconnect.domain.model.CuteConversation
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import com.sosauce.cuteconnect.utils.getContactNameOrNothing

@Composable
fun PinnedConversation(
    modifier: Modifier = Modifier,
    cuteConversation: CuteConversation,
    onNavigate: (Screen) -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean
) {


    val context = LocalContext.current
    val senderOrNumber = remember(cuteConversation.recipients) {
        cuteConversation.recipients.first().getContactNameOrNothing(context)
    }
    Box {
        Box(
            modifier = modifier
                .widthIn(max = 150.dp)
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow)
                .combinedClickable(
                    onClick = { onNavigate(Screen.Conversation(cuteConversation.recipients.first())) },
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DefaultContactIcon(
                    firstLetter = senderOrNumber.firstOrNull(),
                    size = 70.dp
                )
                CuteText(
                    text = senderOrNumber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CuteText(
                    text = cuteConversation.snippet,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AnimatedVisibility(
            visible = cuteConversation.read,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialShapes.Circle.toShape()
                    )
                    .size(15.dp)
            )
        }
    }

}