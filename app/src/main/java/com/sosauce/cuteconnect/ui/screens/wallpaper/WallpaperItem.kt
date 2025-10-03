@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.wallpaper

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cuteconnect.ui.navigation.LocalHazeState
import com.sosauce.cuteconnect.utils.ImageUtils
import com.sosauce.cuteconnect.utils.cuteHazeEffect
import com.sosauce.cuteconnect.utils.rememberHazeState
import com.sosauce.cuteconnect.utils.thenIf
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun CarouselItemScope.WallpaperItem(
    wallpaper: String,
    isCurrentWallpaper: Boolean,
    onSetAsWallpaper: () -> Unit,
    onDeleteWallpaper: () -> Unit,
    blurIntensity: Int
) {
    val context = LocalContext.current
    //val animateBlur by animateIntAsState(blurIntensity)
    Box(
        modifier = Modifier
            .height(180.dp)
            .maskClip(MaterialTheme.shapes.extraLarge)
            .clickable {
                if (isCurrentWallpaper) {
                    onDeleteWallpaper()
                } else {
                    onSetAsWallpaper()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = remember { ImageUtils.imageRequester(wallpaper, context) },
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = isCurrentWallpaper,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier
                    .size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}