@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components.buttons

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.nekobites.animations.AnimatedFab

@Composable
fun CuteNavigationButton(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) {

    CuteNavigationButtonSurface(
        modifier = modifier
            .selfAlignHorizontally(Alignment.Start)
            .navigationBarsPadding()
            .padding(start = 15.dp),
        onNavigateUp = onNavigateUp
    )
}

/**
 * A [CuteNavigationButton] without any modifier pre-applied
 */
@Composable
fun CuteNavigationButtonSurface(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) {
    AnimatedFab(
        modifier = modifier,
        onClick = onNavigateUp,
        icon = R.drawable.back,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}