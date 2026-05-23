@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.phone.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.theme.CinnamonTheme
import com.sosauce.cinnamon.utils.bouncySpec
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun IncomingBottomBar(
    onCallAction: (CallAction) -> Unit
) {


    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val animatable = remember { Animatable(0f) }
    val maxDrag = with(density) { 130.dp.toPx() }
    val dragState = rememberDraggableState { dragAmount ->

        scope.launch {
            val newValue = (animatable.value + dragAmount).coerceIn(-maxDrag, maxDrag)
            animatable.snapTo(newValue)
            println("Drag amount: ${animatable.value}")
        }
    }

    val progress by remember {
        derivedStateOf {
            (animatable.value / maxDrag).coerceIn(-1f, 0f)
        }
    }

    val color by animateColorAsState(
        lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.error,
            -progress
        ),
        bouncySpec()
    )

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(
                horizontal = 20.dp,
            )
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "Decline",
            modifier = Modifier
                .padding(
                    horizontal = 30.dp,
                    vertical = 15.dp
                )
                .align(Alignment.CenterStart),
            color = contentColorFor(MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "Answer",
            modifier = Modifier
                .padding(
                    horizontal = 30.dp,
                    vertical = 15.dp
                )
                .align(Alignment.CenterEnd),
            color = contentColorFor(MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animatable.value.roundToInt(),
                        y = 0
                    )
                }
                .fillMaxWidth(0.3f)
                .height(80.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(50)
                )
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {

                        val threshold = maxDrag * 0.5f

                        val goto = when {
                            animatable.value >= threshold -> {
                                onCallAction(CallAction.AnswerCall)
                                maxDrag
                            }

                            animatable.value <= -threshold -> {
                                onCallAction(CallAction.DeclineCall)
                                -maxDrag
                            }

                            else -> 0f
                        }
                        animatable.animateTo(goto, bouncySpec())
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.phone_filled),
                contentDescription = null,
                tint = contentColorFor(color),
                modifier = Modifier
                    .graphicsLayer {
                        if (animatable.value < 0f) {
                            rotationZ = 135f * (animatable.value / -maxDrag)
                        }
                    }
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun SwipeToAnswerPreview() {

    CinnamonTheme {


        IncomingBottomBar(
            onCallAction = {}
        )
    }
}