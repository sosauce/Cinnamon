@file:OptIn(ExperimentalFoundationApi::class)

package com.sosauce.cinnamon.presentation.screens.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.items.CuteListItem
import com.sosauce.cinnamon.utils.beautifyNumber

@Composable
fun ContactListItem(
    modifier: Modifier = Modifier,
    contact: CuteContact,
    onContactClick: () -> Unit,
    showNumber: Boolean = false
) {


    CuteListItem(
        modifier = modifier,
        onClick = onContactClick,
        leadingContent = {
            DefaultContactIcon(
                firstLetter = contact.displayName.firstOrNull(),
                modifier = Modifier
                    .padding(start = 10.dp),
                contactPfp = contact.photo
            )
        }
    ) {
        Text(
            text = contact.displayName,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
        if (showNumber) {
            Text(
                text = buildString {
                    append(contact.details.phoneNumbers.first().number.beautifyNumber())
                    if (contact.details.phoneNumbers.size > 1) {
                        append(" and ${contact.details.phoneNumbers.size - 1} more")
                    }
                },
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}