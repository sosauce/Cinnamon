package com.sosauce.cinnamon.core.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Transition used by every NavDisplay when navigating to a new screen
 */
fun navForwardTransition(): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally { it } + fadeIn(),
    initialContentExit = slideOutHorizontally { -it / 4 } + fadeOut()
)

/**
 * Transition used by every NavDisplay when going back (regular and predictive)
 */
fun navBackTransition(): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally { -it / 4 } + fadeIn(),
    initialContentExit = slideOutHorizontally { it } + fadeOut()
)
