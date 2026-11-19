package com.sosauce.cinnamon.features.phone.presentation.call.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.nekobites.animations.bouncySpec

/**
 * HD audio / Wi-Fi calling indicators, each one pops in and out on its own.
 *
 * @param containerColor when set, each icon sits in a small tonal chip, otherwise it's drawn bare
 */
@Composable
fun CallQualityIcons(
    isHdAudio: Boolean,
    isWifiCall: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    containerColor: Color? = MaterialTheme.colorScheme.secondaryContainer,
    spacing: Dp = 8.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
    ) {
        QualityIcon(isHdAudio, R.drawable.hd_filled, R.string.hd_call, iconSize, containerColor)
        QualityIcon(isWifiCall, R.drawable.wifi_calling, R.string.wifi_calling, iconSize, containerColor)
    }
}

@Composable
private fun RowScope.QualityIcon(
    visible: Boolean,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    iconSize: Dp,
    containerColor: Color?
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(bouncySpec()) + fadeIn(),
        exit = scaleOut(bouncySpec()) + fadeOut()
    ) {
        if (containerColor != null) {
            Surface(
                shape = CircleShape,
                color = containerColor
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = stringResource(description),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .size(iconSize)
                )
            }
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
                tint = LocalContentColor.current,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
