@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.shared_components

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R

@Composable
fun DefaultContactIcon(
    modifier: Modifier = Modifier,
    firstLetter: Char?,
    size: Dp = 42.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    contactPfp: Uri = Uri.EMPTY,
    shape: Shape = MaterialShapes.Circle.toShape(),
    @DrawableRes icon: Int = R.drawable.person_filled
) {

    val context = LocalContext.current
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                color = color,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (firstLetter?.isLetter() == true) {
            Text(
                text = firstLetter.uppercase(),
                style = MaterialTheme.typography.titleLargeEmphasized.copy(
                    color = contentColorFor(color),
                    fontSize = with(density) { (size / 2).toSp() },
                )
            )
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = contentColorFor(color),
                modifier = Modifier.size(size / 2)
            )
        }
        AsyncImage(
            model = contactPfp,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
        )
    }


}