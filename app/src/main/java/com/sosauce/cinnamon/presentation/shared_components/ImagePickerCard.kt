package com.sosauce.cinnamon.presentation.shared_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R

@Composable
fun ImagePickerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemoveImage: () -> Unit,
    imagePath: String,
    blur: Int = 0
) {
    Box(modifier = modifier) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(24.dp),

        ) {
            if (imagePath.isEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .cloudy(blur)
                )
            }
        }

        AnimatedVisibility(
            visible = imagePath.isNotEmpty(),
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            FilledIconButton(
                onClick = onRemoveImage
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null
                )
            }
        }
    }
}