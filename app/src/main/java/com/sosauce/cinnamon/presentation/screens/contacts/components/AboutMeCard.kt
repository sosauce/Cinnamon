package com.sosauce.cinnamon.presentation.screens.contacts.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon

@Composable
fun AboutMeCard(
    onNavigate: (Screen) -> Unit
) {
    Card(
        onClick = { onNavigate(Screen.AboutMe) },
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DefaultContactIcon(
                firstLetter = 'M',
                size = 48.dp
            )
//            AsyncImage(
//                model = "",
//                contentDescription = null,
//                modifier = Modifier
//                    .size(35.dp)
//                    .clip(CircleShape),
//            )
            Column {
                Text("Me")
                Text(
                    text = "+33700000000",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}