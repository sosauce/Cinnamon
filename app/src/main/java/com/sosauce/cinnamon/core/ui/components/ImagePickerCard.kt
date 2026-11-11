package com.sosauce.cinnamon.core.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R

@Composable
fun ImagePickerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemoveImage: () -> Unit,
    image: Uri?,
    blur: Int = 0
) {
    val chooseImageDescription = stringResource(R.string.choose_image)
    Box(modifier = modifier) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, end = 12.dp)
                .semantics {
                    contentDescription = chooseImageDescription
                },
            shape = RoundedCornerShape(24.dp),

            ) {
            if (image == null) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .cloudy(blur)
                )
            }
        }

        AnimatedVisibility(
            visible = image != null,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            FilledIconButton(
                onClick = onRemoveImage
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.remove_image)
                )
            }
        }
    }
}
