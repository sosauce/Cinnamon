@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.shared_components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.Image
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.utils.ImageUtils

@Composable
fun DefaultContactIcon(
    modifier: Modifier = Modifier,
    firstLetter: Char?,
    size: Dp = 42.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    contactPfp: Uri = Uri.EMPTY,
    shape: Shape = CircleShape
) {

    val context = LocalContext.current
    val density = LocalDensity.current

    if (contactPfp != Uri.EMPTY) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageUtils.imageRequester(contactPfp, context),
                contentDescription = null,
                modifier = Modifier.clip(shape)
            )
        }
    } else {

        Box(
            modifier = modifier
                .size(size)
                .background(
                    color = color,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (firstLetter?.isLetter() == true) {
                CuteText(
                    text = firstLetter.uppercase(),
                    style = MaterialTheme.typography.titleLargeEmphasized.copy(
                        color = MaterialTheme.colorScheme.contentColorFor(color),
                        fontSize = with(density) { (size / 2).toSp() },
                    )
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.default_pfp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.contentColorFor(color),
                )
            }
        }
    }


}