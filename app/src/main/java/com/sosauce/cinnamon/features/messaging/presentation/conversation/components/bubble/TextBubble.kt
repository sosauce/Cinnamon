package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
    bubbleColor: Color,
    highlightQuery: String? = null,
    isFocusedMatch: Boolean = false
) {
    TextBubbleContent(
        text = body,
        type = type,
        bubbleColor = bubbleColor,
        sandwichPosition = sandwichPosition,
        isScheduled = isScheduled,
        highlightQuery = highlightQuery,
        isFocusedMatch = isFocusedMatch
    )
}

/**
 * @param highlightQuery occurrences of it (case-insensitive) get highlighted, used by conversation search
 * @param isFocusedMatch whether this bubble is the currently focused search match, highlighted stronger
 */
@Composable
fun TextBubbleContent(
    text: String,
    type: MessageType,
    isScheduled: Boolean,
    sandwichPosition: SandwichPosition,
    bubbleColor: Color,
    highlightQuery: String? = null,
    isFocusedMatch: Boolean = false
) {

    val uriHandler = LocalUriHandler.current
    val highlightContainer = if (isFocusedMatch) {
        MaterialTheme.colorScheme.tertiary
    } else MaterialTheme.colorScheme.tertiaryContainer
    val highlightContent = contentColorFor(highlightContainer)
    val displayedText = remember(text, highlightQuery, highlightContainer, highlightContent) {
        text.highlightOccurrences(
            query = highlightQuery,
            style = SpanStyle(
                background = highlightContainer,
                color = highlightContent
            )
        )
    }
    val shape = BubbleShape(
        sandwichPosition = sandwichPosition,
        messageType = type
    )
    val bodyLarge = MaterialTheme.typography.labelLargeEmphasized

    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                color = bubbleColor,
                //shape = shape
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
            text = displayedText,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            style = bodyLarge.copy(
                fontSize = if (text.isEmoji()) 35.sp else bodyLarge.fontSize,
                lineHeight = if (text.isEmoji()) 42.sp else bodyLarge.lineHeight,
                color = contentColorFor(bubbleColor),
                textDecoration = if (text.isLink()) TextDecoration.Underline else null,
            )
        )
    }
}
private fun String.highlightOccurrences(
    query: String?,
    style: SpanStyle
): AnnotatedString = buildAnnotatedString {
    append(this@highlightOccurrences)
    if (query.isNullOrEmpty()) return@buildAnnotatedString

    var start = indexOf(query, ignoreCase = true)
    while (start >= 0) {
        addStyle(style, start, start + query.length)
        start = indexOf(query, startIndex = start + query.length, ignoreCase = true)
    }
}
