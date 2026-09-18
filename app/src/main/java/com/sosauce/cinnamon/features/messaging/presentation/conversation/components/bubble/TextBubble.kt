package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sosauce.cinnamon.core.utils.isEmoji
import com.sosauce.cinnamon.core.utils.isLink
import com.sosauce.cinnamon.core.utils.thenIf
import com.sosauce.cinnamon.features.messaging.domain.MessageType
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.dashedBorder

@Composable
fun TextBubble(
    body: String,
    type: MessageType,
    sandwichPosition: SandwichPosition,
    isScheduled: Boolean,
    bubbleColor: Color
) {
    TextBubbleContent(
        text = body,
        type = type,
        bubbleColor = bubbleColor,
        sandwichPosition = sandwichPosition,
        isScheduled = isScheduled
    )
}

@Composable
fun TextBubbleContent(
    text: String,
    type: MessageType,
    isScheduled: Boolean,
    sandwichPosition: SandwichPosition,
    bubbleColor: Color
) {

    val uriHandler = LocalUriHandler.current
    val shape = BubbleShape(
        sandwichPosition = sandwichPosition,
        messageType = type
    )

    Box(
        modifier = Modifier
            .background(
                color = bubbleColor,
                shape = shape
            )
            .thenIf(text.isLink()) {
                Modifier.clickable {
                    uriHandler.openUri(text)
                }
            }
            .then(
                if (isScheduled) {
                    Modifier.dashedBorder(shape)
                } else Modifier
            )

    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(10.dp),
            style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                fontSize = if (text.isEmoji()) 35.sp else TextUnit.Unspecified,
                color = contentColorFor(bubbleColor),
                textDecoration = if (text.isLink()) TextDecoration.Underline else null,
            )
        )
    }
}