package com.sosauce.cinnamon.core.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

val barsContentTransform = ContentTransform(
    targetContentEnter = slideInVertically(
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) { it } + fadeIn(),
    initialContentExit = slideOutVertically(
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) { it } + fadeOut(),
    sizeTransform = SizeTransform(clip = false)
)
