@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components.menus

import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.MoreOptions

/**
 * Popup menu listing [options], it is dismissed once an option is clicked
 */
@Composable
fun MoreOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<MoreOptions>
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShapes()
        ) {
            options.fastForEachIndexed { index, option ->
                DropdownMenuItem(
                    onClick = {
                        option.onClick()
                        onDismissRequest()
                    },
                    shape = MenuDefaults.getItemShape(index, options.lastIndex),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                            tint = option.tint ?: LocalContentColor.current
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(option.text),
                            color = option.tint ?: LocalContentColor.current
                        )
                    }
                )
            }
        }
    }
}
