@file:OptIn(ExperimentalMaterial3Api::class)

package com.sosauce.cinnamon.presentation.shared_components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.utils.rememberInteractionSource

@Composable
fun AnimatedSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChanged: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: SliderColors = SliderDefaults.colors()
) {
    val interactionSource = rememberInteractionSource()
    val isDragging by interactionSource.collectIsDraggedAsState()
    val animatedValue by animateFloatAsState(value)

    Slider(
        modifier = modifier,
        value = animatedValue,
        onValueChange = onValueChanged,
        onValueChangeFinished = onValueChangeFinished,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                drawStopIndicator = null,
                thumbTrackGapSize = 4.dp,
                modifier = Modifier.height(5.dp),
                trackInsideCornerSize = 3.dp
            )
        },
        thumb = {

            val width by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 4.dp
            )

            SliderDefaults.Thumb(
                interactionSource = rememberInteractionSource(),
                thumbSize = DpSize(width = width, height = 30.dp)
            )
        },
        valueRange = valueRange,
        interactionSource = interactionSource,
        colors = colors
    )
}