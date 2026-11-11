@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.domain.CuteConversation


@Composable
fun ConversationIcon(
    conversation: CuteConversation,
    size: Dp = 50.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialShapes.Circle.toShape())
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {

        val firstChar = conversation.name.first()

        if (firstChar.isLetter()) {
            Text(
                text = firstChar.toString(),
                style = MaterialTheme.typography.titleLargeEmphasized.copy(
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.person_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        AsyncImage(
            model = conversation.photo,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
