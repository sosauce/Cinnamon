@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)

package com.sosauce.cinnamon.presentation.shared_components.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

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
            .selfAlignHorizontally()
            .fillMaxWidth(0.95f)
            .systemBarsPadding()
            .clip(FloatingToolbarDefaults.ContainerShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
//            .hazeEffect(
//                state = LocalHazeState.current,
//                style = HazeMaterials.regular(
//                    containerColor = MaterialTheme.colorScheme.surfaceContainer
//                )
//            )
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = Color.Transparent
        ),
        content = content
    )
}