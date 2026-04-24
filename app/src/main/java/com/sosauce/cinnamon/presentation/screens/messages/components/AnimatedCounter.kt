package com.sosauce.cinnamon.presentation.screens.messages.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.skydoves.cloudy.cloudy

@Composable
fun AnimatedCounter(
    count: Int
) {
    val scope = rememberCoroutineScope()
    var oldCount by remember { mutableIntStateOf(count) }

    LaunchedEffect(count) {
        oldCount = count
    }

    Row {
        val countString = count.toString()
        val oldCountString = oldCount.toString()
        for (i in countString.indices) {
            val oldChar = oldCountString.getOrNull(i)
            val newChar = countString[i]

            val char = if (oldChar == newChar) {
                oldCountString[i]
            } else countString[i]


            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (oldCount < count) {
                        slideInVertically { it } togetherWith slideOutVertically { -it }
                    } else {
                        slideInVertically { -it } togetherWith slideOutVertically { it }
                    }
                }
            ) {

                val radius by animateIntAsState(
                    targetValue = if (transition.isRunning) 15 else 0,
                    animationSpec = tween(500)
                )

                Text(
                    text = it.toString(),
                    modifier = Modifier.cloudy(radius)
                )
            }
        }
    }

}