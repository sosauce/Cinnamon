package com.sosauce.cuteconnect.ui.screens.phone.components

import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import com.sosauce.cuteconnect.data.actions.CallAction
import com.sosauce.cuteconnect.domain.model.AudioRoute
import com.sosauce.cuteconnect.ui.shared_components.CuteDropdownMenuItem
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText

@Composable
fun AudioSwitcher(
    onCallAction: (CallAction) -> Unit,
    routes: List<AudioRoute>
) {
    LazyColumn {
        items(
            items = routes
        ) { route ->
            CuteDropdownMenuItem(
                modifier = Modifier.animateItem(),
                onClick = { onCallAction(CallAction.SwitchAudioTarget(route)) },
                text = { CuteText(route.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingIcon = {
                    Icon(
                        imageVector = route.type.routeToIcon(),
                        contentDescription = null
                    )
                }
            )
        }
    }
}