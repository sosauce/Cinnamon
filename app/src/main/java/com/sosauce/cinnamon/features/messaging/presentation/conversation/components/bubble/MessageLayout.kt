package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.cinnamon.core.utils.rememberInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.domain.MessageType

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MessageLayout(
    modifier: Modifier = Modifier,
    message: CuteMessage,
    sandwichPosition: SandwichPosition,
    isSelected: Boolean,
    isTimestampVisible: Boolean,
    isGroupChat: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    statusContent: @Composable (ColumnScope.() -> Unit)? = null,
    bubbleContent: @Composable () -> Unit
) {

    val config = LocalConfiguration.current
    val alignment =
        if (message.type == MessageType.RECEIVED) Alignment.Start else Alignment.End
    val verticalPadding = when (sandwichPosition) {
        SandwichPosition.SOLO -> 5.dp
        else -> 1.dp
    }
    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val bubbleScale by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 0.95f else 1f,
        animationSpec = bouncySpec()
    )
    val selectionColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    )


    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .background(selectionColor, RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 5.dp, vertical = verticalPadding)

    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .widthIn(max = config.screenWidthDp.dp * 0.8f)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
        ) {
            bubbleContent()
        }
        AnimatedVisibility(
            visible = isTimestampVisible,
            enter = expandVertically(),
            exit = shrinkVertically(),
            modifier = Modifier
                .padding(5.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(50)
                )
                .align(alignment)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                when {
                    message.delivered -> {
                        Icon(
                            painter = painterResource(R.drawable.delivered),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(18.dp)
                        )
                    }

                    message.isScheduled -> {
                        Icon(
                            painter = painterResource(R.drawable.timer),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(18.dp)
                        )
                    }
                }
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }
        }
        statusContent?.let {
            Box(Modifier.align(alignment)) {
                it(this@Column)
            }
        }
        if (isGroupChat && (sandwichPosition == SandwichPosition.BOTTOM || sandwichPosition == SandwichPosition.SOLO) && message.type == MessageType.RECEIVED) {
            DefaultContactIcon(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .align(alignment),
                firstLetter = 'S',
                size = 30.dp
            )
        }
    }
}

enum class SandwichPosition {
    SOLO, TOP, MIDDLE, BOTTOM
}

@Composable
fun BubbleShape(
    sandwichPosition: SandwichPosition,
    messageType: MessageType
): Shape {


    return if (messageType == MessageType.SENT) {

        val topEnd by animateDpAsState(
            when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.TOP -> 24.dp
                else -> 4.dp
            }
        )

        val bottomEnd by animateDpAsState(
            when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.BOTTOM -> 24.dp
                else -> 4.dp
            }
        )

        RoundedCornerShape(
            topStart = 24.dp,
            bottomStart = 24.dp,
            topEnd = topEnd,
            bottomEnd = bottomEnd
        )
    } else {

        val topStart by animateDpAsState(
            when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.TOP -> 24.dp
                else -> 4.dp
            }
        )

        val bottomStart by animateDpAsState(
            when (sandwichPosition) {
                SandwichPosition.SOLO, SandwichPosition.BOTTOM -> 24.dp
                else -> 4.dp
            }
        )

        RoundedCornerShape(
            topStart = topStart,
            bottomStart = bottomStart,
            topEnd = 24.dp,
            bottomEnd = 24.dp
        )
    }
}