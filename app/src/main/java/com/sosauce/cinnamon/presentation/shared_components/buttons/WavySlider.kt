package com.sosauce.cinnamon.presentation.shared_components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.utils.rememberInteractionSource

@Composable
fun WavySlider(
    modifier: Modifier = Modifier,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (Float) -> Unit,
    colors: SliderColors = SliderDefaults.colors()
) {


    val state = rememberSliderState(
        value = value,
        valueRange = valueRange
    )
    state.onValueChangeFinished = { onValueChangeFinished(state.value) }

    LaunchedEffect(value) {
        if (!state.isDragging) {
            state.value = value
        }
    }

    Slider(
        modifier = modifier,
        state = state,
        colors = colors,
        thumb = {
            val animatedHeight by animateDpAsState(
                if (it.isDragging) 40.dp else 35.dp
            )

            val animatedWidth by animateDpAsState(
                if (it.isDragging) 10.dp else 6.dp
            )

            SliderDefaults.Thumb(
                interactionSource = rememberInteractionSource(),
                thumbSize = DpSize(animatedWidth, animatedHeight)
            )
        },
        track = { sliderState ->
            val animatedHeight by animateDpAsState(
                if (sliderState.isDragging) 7.dp else 4.dp
            )
            val trackStroke = Stroke(
                width =
                    with(LocalDensity.current) {
                        animatedHeight.toPx()
                    },
                cap = StrokeCap.Round,
            )

            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = {
                    val rangeLength = valueRange.endInclusive - valueRange.start
                    if (rangeLength > 0f) {
                        (sliderState.value - valueRange.start) / rangeLength
                    } else 0f
                },
                stopSize = 0.dp,
                trackStroke = trackStroke,
                amplitude = { if (!sliderState.isDragging) 1f else 0f }
            )
        }
    )
}