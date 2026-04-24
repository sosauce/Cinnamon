package com.sosauce.cinnamon.presentation.shared_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import sv.lib.squircleshape.CornerSmoothing
import sv.lib.squircleshape.SquircleShape

@Composable
fun AppIcon(
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .padding(15.dp)
            .background(
                shape = SquircleShape(smoothing = CornerSmoothing.Full),
                color = Color(0xFFf4a7bd)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = null,
            modifier = Modifier.size(size / 2),
            tint = Color(0xFFfdd9dc)
        )
    }
}