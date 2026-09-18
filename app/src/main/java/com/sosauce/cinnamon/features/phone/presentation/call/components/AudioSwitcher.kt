@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.features.phone.domain.AudioRoute
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction

@Composable
fun AudioSwitcher(
    onCallAction: (CallAction) -> Unit,
    routes: List<AudioRoute>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                itemsIndexed(
                    items = routes
                ) { index, route ->
                    CuteListItem(
                        onClick = { onCallAction(CallAction.SwitchAudioTarget(route)) },
                        shape = MenuDefaults.getItemShape(index, routes.lastIndex),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        leadingContent = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(route.type.routeToIcon()),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    ) {
                        Text(
                            text = route.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmallEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}
