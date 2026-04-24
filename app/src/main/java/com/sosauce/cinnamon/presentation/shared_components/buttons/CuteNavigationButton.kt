@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.shared_components.buttons

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.utils.selfAlignHorizontally

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
    FloatingActionButton(
        onClick = onNavigateUp,
        modifier = modifier,
        shape = MaterialShapes.Cookie9Sided.toShape(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Icon(
            painter = painterResource(R.drawable.back),
            contentDescription = null
        )
    }
}