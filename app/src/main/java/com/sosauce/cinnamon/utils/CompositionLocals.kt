package com.sosauce.cinnamon.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import dev.chrisbanes.haze.HazeState

val LocalScreen =
    compositionLocalOf<NavKey> { throw IllegalStateException("Current screen can't be determined!") }
val LocalHazeState = staticCompositionLocalOf { HazeState() }