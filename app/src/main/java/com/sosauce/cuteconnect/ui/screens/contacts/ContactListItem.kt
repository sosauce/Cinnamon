@file:OptIn(ExperimentalFoundationApi::class)

package com.sosauce.cuteconnect.ui.screens.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sosauce.cuteconnect.domain.model.CuteContact
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import com.sosauce.cuteconnect.utils.betterFormatNumber
import com.sosauce.cuteconnect.utils.thenIf

@Composable
fun ContactListItem(
    modifier: Modifier = Modifier,
    contact: CuteContact,
    onContactClick: () -> Unit,
    showNumber: Boolean = false
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onContactClick
            )
            .thenIf(!showNumber) {
                padding(vertical = 10.dp)
            }
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultContactIcon(
                firstLetter = contact.name.first(),
                modifier = Modifier
                    .padding(start = 10.dp),
                contactPfp = contact.photo
            )
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                CuteText(
                    text = contact.name,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                if (showNumber) {
                    CuteText(
                        text = contact.phoneNumbers.first().number.betterFormatNumber(),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}