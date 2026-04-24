@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.phone.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.sosauce.cinnamon.domain.model.AudioRoute
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.shared_components.items.CuteListItem
import com.sosauce.cinnamon.utils.getItemShape

@Composable
fun AudioSwitcher(
    onCallAction: (CallAction) -> Unit,
    routes: List<AudioRoute>
) {
    LazyColumn {
        itemsIndexed(
            items = routes
        ) { index, route ->
            CuteListItem(
                onClick = { onCallAction(CallAction.SwitchAudioTarget(route)) },
                shape = MenuDefaults.getItemShape(index, routes.lastIndex),
                leadingContent = {
                    Icon(
                        painter = painterResource(route.type.routeToIcon()),
                        contentDescription = null
                    )
                }
            ) { Text(route.name.lowercase().replaceFirstChar { it.uppercase() }) }
        }
    }
}