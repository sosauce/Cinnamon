package com.sosauce.cinnamon.presentation.screens.messages.components.bubble

import android.provider.Telephony
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.screens.messages.components.bottombar.dashedBorder
import com.sosauce.cinnamon.utils.isEmoji
import com.sosauce.cinnamon.utils.isLink
import com.sosauce.cinnamon.utils.thenIf

@Composable
fun TextBubble(
    body: String,
    type: Int,
    sandwichPosition: SandwichPosition,
    isScheduled: Boolean,
    bubbleColor: Color
) {
    if (body == "😭") {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.crying)
        )
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(35.dp)
        )
    } else {
        TextBubbleContent(
            text = body,
            type = type,
            bubbleColor = bubbleColor,
            sandwichPosition = sandwichPosition,
            isScheduled = isScheduled
        )
    }
}

@Composable
fun TextBubbleContent(
    text: String,
    type: Int,
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