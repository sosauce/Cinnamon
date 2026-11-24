package com.sosauce.cinnamon.core.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey
import com.sosauce.cinnamon.app.navigation.Screen

val LocalScreen =
    compositionLocalOf<Screen> { throw IllegalStateException("Current screen can't be determined!") }
