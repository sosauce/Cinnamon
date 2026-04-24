@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.phone.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.screens.phone.CallAction

@Composable
fun IncomingBottomBar(
    onCallAction: (CallAction) -> Unit
) {

    val interactionSources = List(2) { remember { MutableInteractionSource() } }

    ButtonGroup(
        overflowIndicator = {},
        modifier = Modifier.padding(bottom = 50.dp)
    ) {
        customItem(
            {
                IconButton(
                    onClick = { onCallAction(CallAction.DeclineCall) },
                    interactionSource = interactionSources[0],
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.error)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                        .animateWidth(interactionSources[0])
                ) {
                    Icon(
                        painter = painterResource(R.drawable.phone_filled),
                        contentDescription = null,
                        modifier = Modifier.rotate(135f)
                    )
                }
            },
            {}
        )
        customItem(
            {
                IconButton(
                    onClick = { onCallAction(CallAction.AnswerCall) },
                    interactionSource = interactionSources[1],
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primary)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                        .animateWidth(interactionSources[1])
                ) {
                    Icon(
                        painter = painterResource(R.drawable.phone_filled),
                        contentDescription = null
                    )
                }
            },
            {}
        )
    }
}