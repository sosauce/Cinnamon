@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.nekobites.components.AnimatedSelectedIcon

@Composable
fun SharedTransitionScope.ContactListItem(
    modifier: Modifier = Modifier,
    contact: CuteContact,
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
                DefaultContactIcon(
                    firstLetter = contact.displayName.firstOrNull(),
                    contactPfp = contact.thumbnail
                )
            }
        }
    ) {
        Text(
            text = contact.displayName,
            style = MaterialTheme.typography.titleMediumEmphasized,
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
                style = MaterialTheme.typography.bodyMediumEmphasized,
                modifier = Modifier
                    .basicMarquee(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}