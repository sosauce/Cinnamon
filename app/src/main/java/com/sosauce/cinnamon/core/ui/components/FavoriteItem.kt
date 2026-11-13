package com.sosauce.chocola.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.sosauce.cinnamon.core.ui.components.SelectionCheckbox
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.components.Spacer
import com.sosauce.nekobites.helpers.rememberInteractionSource


@Composable
fun FavoriteItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    favoriteBubble: @Composable (() -> Unit)?,
    extraBadge: @Composable (() -> Unit)?= null,
    visual: @Composable BoxScope.(isPressed: Boolean) -> Unit
) {
    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 0.95f else 1f,
        animationSpec = bouncySpec()
    )
    val offset = DpOffset(x = 6.dp, y = 6.dp)

    Box(
        modifier = modifier
            .reserveBadgeSpace(offset)
            .width(135.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .combinedClickable(
                    interactionSource = interactionSource,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                visual(isPressed)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMediumEmphasized.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.basicMarquee(),
                maxLines = 1
            )

        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .badgeOffset(offset),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedVisibility(
                visible = !isInSelectionMode,
                enter = scaleIn(bouncySpec()),
                exit = scaleOut(bouncySpec())
            ) {
                favoriteBubble?.invoke()
            }
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = !isInSelectionMode
                ) {
                    if (it) {
                        extraBadge?.invoke()
                    } else {
                        SelectionCheckbox(
                            visible = isInSelectionMode,
                            checked = isSelected
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FavoriteBubble(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int
) {

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 3.dp, y = 3.dp)
                .size(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialShapes.Circle.toShape()
                )
        )
        Box(
            modifier = Modifier
                .size(25.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialShapes.Circle.toShape()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Makes the row [x] wider on each side and moves it [y] up, so its content sits over the card's edge like a badge.
 */
private fun Modifier.badgeOffset(offset: DpOffset): Modifier = layout { measurable, constraints ->
    val extra = (offset.x * 2).roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(x = -(offset.x).roundToPx(), y = -(offset.y).roundToPx())
    }
}

/**
 * Reserves the offset so the parent of a child element that uses [badgeOffset] can take this offset into final measuring
 */
fun Modifier.reserveBadgeSpace(offset: DpOffset): Modifier = layout { measurable, constraints ->
    val xPx = offset.x.roundToPx()
    val yPx = offset.y.roundToPx()
    val placeable = measurable.measure(constraints.offset(-xPx, -yPx))
    layout(placeable.width + xPx, placeable.height + yPx) {
        placeable.place(0, yPx)
    }
}