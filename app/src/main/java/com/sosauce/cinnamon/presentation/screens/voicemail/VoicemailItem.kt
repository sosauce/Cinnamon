package com.sosauce.cinnamon.presentation.screens.voicemail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.domain.model.CuteVoicemail
import com.sosauce.cinnamon.presentation.shared_components.AnimatedSlider
import com.sosauce.cinnamon.utils.getContactNameOrNothing

@Composable
fun VoicemailItem(
    voicemail: CuteVoicemail
) {

    val context = LocalContext.current
    val nameOrNumber = remember { voicemail.address.getContactNameOrNothing(context) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val innerPadding by animateDpAsState(
        targetValue = if (expanded) 10.dp else 0.dp
    )


    Card(
        modifier = Modifier.padding(5.dp),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            // TODO
//            DropdownMenuItemUnclickable(
//                text = {
//                    Column {
//                        Text(nameOrNumber)
//                        Text(
//                            text = "${voicemail.date.toReadableDate()} · ${voicemail.duration.toReadableDuration()}",
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//
//                    }
//                },
//                leadingIcon = {
//                    DefaultContactIcon(
//                        firstLetter = nameOrNumber.firstOrNull(),
//                        contactPfp = voicemail.address.getContactPfpUri(context)
//                    )
//                },
//                trailingIcon = {
//                    PlayPauseButton(
//                        isPlaying = AudioManager.isPlaying
//                    ) {
//                        AudioManager.setMediaItem(voicemail.uri)
//                        AudioManager.playOrPause()
//                    }
//                }
//            )
            if (expanded) {
                AnimatedSlider(
                    value = 0f,
                    onValueChanged = {},
                    valueRange = 0f..1f,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}