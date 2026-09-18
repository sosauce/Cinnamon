package com.sosauce.cinnamon.core.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


@Composable
fun rememberSeedColor(
    isDark: Boolean
): Color {
    return if (isDark) anyDarkColorScheme().primary else anyLightColorScheme().primary
}

@Composable
fun anyLightColorScheme(): ColorScheme {
    val context = LocalContext.current

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme()
    }
}

@Composable
fun anyDarkColorScheme(): ColorScheme {
    val context = LocalContext.current

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme()
    }
}

object ChatColor {
    var color by mutableStateOf<Color?>(null)
}