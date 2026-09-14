package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

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
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.domain.MessageType
import sv.lib.squircleshape.GentleSquircleShape

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


    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = verticalPadding)

    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .widthIn(max = config.screenWidthDp.dp * 0.8f)
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
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(50)
                )
                .align(alignment)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    message.delivered -> {
                        Icon(
                            painter = painterResource(R.drawable.delivered),
                            contentDescription = null
                        )
                    }

                    message.isScheduled -> {
                        Icon(
                            painter = painterResource(R.drawable.timer),
                            contentDescription = null
                        )
                    }
                }
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.bodyMediumEmphasized
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