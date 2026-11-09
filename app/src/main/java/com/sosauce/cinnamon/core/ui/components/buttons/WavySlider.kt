package com.sosauce.cinnamon.core.ui.components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.core.utils.rememberInteractionSource

@Composable
fun WavySlider(
    modifier: Modifier = Modifier,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = false
) {

    val interactionSource = rememberInteractionSource()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val displayedValue = remember { Animatable(value) }

    LaunchedEffect(value) {
        if (isDragged || isPressed) {
            displayedValue.snapTo(value)
        } else {
            displayedValue.animateTo(value)
        }
    }

    val state = rememberSliderState(
        value = value,
        trackRange = valueRange
    )
    state.value = if (isDragged || isPressed) value else displayedValue.value

    Slider(
        modifier = modifier,
        state = state,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            val animatedHeight by animateDpAsState(if (it.isDragging) 40.dp else 35.dp)
            val animatedWidth by animateDpAsState(if (it.isDragging) 10.dp else 6.dp)

            SliderDefaults.Thumb(
                interactionSource = interactionSource,
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
                    val rangeLength = sliderState.trackRange.endInclusive - sliderState.trackRange.start
                    if (rangeLength > 0f) {
                        (sliderState.value - sliderState.trackRange.start) / rangeLength
                    } else 0f
                },
                stopSize = 0.dp,
                trackStroke = trackStroke,
                amplitude = { if (isPlaying && !sliderState.isDragging) 1f else 0f }
            )
        }
    )
}