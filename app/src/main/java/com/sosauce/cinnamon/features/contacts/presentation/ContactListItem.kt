@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.features.contacts.data.model.CuteContact
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.features.contacts.domain.CuteContact2
import com.sosauce.nekobites.components.AnimatedSelectedIcon

@Composable
fun SharedTransitionScope.ContactListItem(
    modifier: Modifier = Modifier,
    contact: CuteContact2,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showNumber: Boolean = false
) {


    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f
    )

    CuteListItem(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            AnimatedSelectedIcon(
                isSelected = isSelected
            ) {
                Box(
                    modifier = modifier
                        .size(50.dp)
                        .clip(MaterialShapes.Circle.toShape())
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {

                    val firstLetter = contact.displayName.firstOrNull() ?: '?'

                    if (firstLetter.isLetter()) {
                        Text(
                            text = firstLetter.uppercase(),
                            style = MaterialTheme.typography.titleLargeEmphasized.copy(
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person_filled),
                            contentDescription = null,
                            tint = contentColorFor(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                    AsyncImage(
                        model = contact.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    ) {
        Text(
            text = contact.displayName,
            maxLines = 1,
            modifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_NAME + contact.id),
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                )
                .basicMarquee()
        )
        if (showNumber) {
            Text(
                text = buildString {
                    append(contact.phoneNumbers.first().number)
                    if (contact.phoneNumbers.size > 1) {
                        append(" and ${contact.phoneNumbers.size - 1} more")
                    }
                },
                maxLines = 1,
                modifier = Modifier
                    .basicMarquee(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}