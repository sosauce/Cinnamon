package com.sosauce.cinnamon.features.contacts.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sosauce.chocola.core.designsystem.components.FavoriteBubble
import com.sosauce.chocola.core.designsystem.components.FavoriteItem
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.features.contacts.domain.CuteContact

@Composable
fun FavoriteContact(
    modifier: Modifier = Modifier,
    contact: CuteContact,
    showNumber: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isInSelectionMode: Boolean = false
) {

    val subtitle = if (showNumber) {
        buildString {
            append(contact.phoneNumbers.first().number)
            if (contact.phoneNumbers.size > 1) {
                append(" and ${contact.phoneNumbers.size - 1} more")
            }
        }
    } else ""

    FavoriteItem(
        modifier = modifier,
        title = contact.displayName,
        subtitle = subtitle,
        onClick = onClick,
        onLongClick = onLongClick,
        isSelected = isSelected,
        isInSelectionMode = isInSelectionMode,
        favoriteBubble = { FavoriteBubble(icon = R.drawable.favorite_filled) },
        visual = {
            DefaultContactIcon(
                firstLetter = contact.displayName.firstOrNull(),
                contactPfp = contact.thumbnail,
                size = 90.dp
            )
        }
    )
}