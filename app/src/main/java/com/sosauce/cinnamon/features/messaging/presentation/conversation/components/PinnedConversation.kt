@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.CuteAvatar
import com.sosauce.cinnamon.core.ui.components.DefaultGroupChatIcon
import com.sosauce.cinnamon.core.ui.components.SelectionCheckbox
import com.sosauce.cinnamon.core.utils.rememberInteractionSource
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation
import com.sosauce.nekobites.animations.bouncySpec

@Composable
fun PinnedConversation(
    modifier: Modifier = Modifier,
    conversation: CuteConversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isInSelectionMode: Boolean = false
) {
    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 0.95f else 1f,
        animationSpec = bouncySpec()
    )

    Box(
        modifier = modifier
            .width(135.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .combinedClickable(
                    interactionSource = interactionSource,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(90.dp)
            ) {
                if (conversation.isGroupChat) {
                    DefaultGroupChatIcon(size = 90.dp)
                } else {
                    CuteAvatar(
                        name = conversation.name,
                        photo = conversation.photo,
                        size = 90.dp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = conversation.name,
                style = MaterialTheme.typography.titleSmallEmphasized,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = conversation.snippet,
                style = MaterialTheme.typography.labelMediumEmphasized.copy(
                    fontStyle = if (conversation.isAnyBlocked && !conversation.isGroupChat) FontStyle.Italic else FontStyle.Normal,
                    color = if (conversation.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .badgeOffset(x = 6.dp, y = 6.dp)
        ) {
            AnimatedVisibility(
                visible = !isInSelectionMode,
                enter = scaleIn(bouncySpec()),
                exit = scaleOut(bouncySpec())
            ) {
                PinnedBubble(unread = !conversation.read)
            }
            Spacer(Modifier.weight(1f))
            SelectionCheckbox(
                visible = isInSelectionMode,
                checked = isSelected
            )
        }
    }
}

@Composable
private fun PinnedBubble(unread: Boolean) {
    val colors = MaterialTheme.colorScheme

    Box {
        AnimatedVisibility(
            visible = unread,
            enter = scaleIn(bouncySpec()),
            exit = scaleOut(bouncySpec()),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 3.dp, y = 3.dp)
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(colors.tertiary, MaterialShapes.Circle.toShape())
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(25.dp)
                .background(colors.primary, MaterialShapes.Circle.toShape())
        ) {
            Icon(
                painter = painterResource(R.drawable.pin_filled),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun Modifier.badgeOffset(x: Dp, y: Dp): Modifier = layout { measurable, constraints ->
    val extra = (x * 2).roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(x = -x.roundToPx(), y = -y.roundToPx())
    }
}
