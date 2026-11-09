package com.sosauce.cinnamon.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R

@Composable
fun CuteAvatar(
    name: String,
    photo: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    shape: Shape = CircleShape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    @DrawableRes placeholder: Int = R.drawable.person_filled
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
    ) {
        val firstChar = name.firstOrNull()

        if (firstChar?.isLetter() == true) {
            Text(
                text = firstChar.uppercase(),
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = contentColor
            )
        } else {
            Icon(
                painter = painterResource(placeholder),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(size / 2)
            )
        }
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
