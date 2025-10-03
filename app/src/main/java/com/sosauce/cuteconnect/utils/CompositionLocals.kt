package com.sosauce.cuteconnect.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey

val LocalScreen = compositionLocalOf<NavKey> { throw IllegalStateException("Current screen can't be determined!") }