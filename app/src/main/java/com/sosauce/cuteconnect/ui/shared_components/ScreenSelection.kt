@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.shared_components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.navigation3.runtime.NavKey
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.utils.LocalScreen
import com.sosauce.cuteconnect.utils.rememberInteractionSource

@Composable
fun ScreenSelection(
    screenToLeadingIcon: Map<Screen, Int>,
    onNavigate: (Screen) -> Unit,
    dismiss: () -> Unit
) {

    val interactionsSources = List(4) { rememberInteractionSource() }
    val currentScreen = LocalScreen.current


    ButtonGroup(
        modifier = Modifier.fillMaxWidth()
    ) {
        screenToLeadingIcon.onEachIndexed { index, (screen, icon) ->
            ToggleButton(
                checked = currentScreen == screen,
                onCheckedChange = {
                    onNavigate(screen)
                    dismiss()
                },
                shapes = ToggleButtonDefaults.shapes(),
                interactionSource = interactionsSources[index],
                modifier = Modifier
                    .weight(1f)
                    .animateWidth(interactionsSources[index])
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }
        }
    }
}