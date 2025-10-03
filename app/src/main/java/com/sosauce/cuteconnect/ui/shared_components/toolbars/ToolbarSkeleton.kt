@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)

package com.sosauce.cuteconnect.ui.shared_components.toolbars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sosauce.cuteconnect.ui.navigation.LocalHazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * This should be used for any toolbar, it already applies padding, width and support for haze blur.
 */
@Composable
fun ToolbarSkeleton(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable (RowScope.() -> Unit)
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier
            .padding(horizontal = 5.dp)
            .fillMaxWidth()
            .systemBarsPadding()
            .clip(FloatingToolbarDefaults.ContainerShape)
            .hazeEffect(
                state = LocalHazeState.current,
                style = HazeMaterials.regular(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
            .clickable { onClick?.invoke() },
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = Color.Transparent
        ),
        content = content
    )
}