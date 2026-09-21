package com.sosauce.cinnamon.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberDefaultMessagesSim
import com.sosauce.cinnamon.core.datastore.rememberEnableDeliveryReports
import com.sosauce.cinnamon.core.datastore.rememberEnablePinchToZoom
import com.sosauce.cinnamon.core.datastore.rememberSendGroupAsMms
import com.sosauce.cinnamon.core.datastore.rememberSendLongAsMms
import com.sosauce.cinnamon.core.datastore.rememberShowCharCount
import com.sosauce.cinnamon.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.settings.components.SimSelector
import com.sosauce.cinnamon.settings.components.SwitchSettingsCard
import com.sosauce.nekobites.components.LazyRowWithScrollButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsMessages() {

    var sendGroupAsMms by rememberSendGroupAsMms()
    var enableDeliveryReports by rememberEnableDeliveryReports()
    var sendLongAsMms by rememberSendLongAsMms()
    val simsViewModel = koinViewModel<SimsViewModel>()
    var defaultMessagesSims by rememberDefaultMessagesSim()
    var showCharCount by rememberShowCharCount()
    var pinchToZoom by rememberEnablePinchToZoom()


    Column {
        SettingsWithTitle(title = R.string.messages) {
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomEnd = 2.dp,
                    bottomStart = 2.dp
                )
            ) {
                LazyRowWithScrollButton(
                    items = simsViewModel.fetchSims()
                ) { sim ->
                    SimSelector(
                        simCard = sim,
                        isDefaultSim = defaultMessagesSims == sim.subId,
                        onClick = { defaultMessagesSims = sim.subId }
                    )
                }
                Text(
                    text = "Default sim for messaging functions.",
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(5.dp)
                )
            }
            SwitchSettingsCard(
                checked = pinchToZoom,
                onCheckedChange = { pinchToZoom = !pinchToZoom },
                topDp = 2.dp,
                bottomDp = 2.dp,
                text = stringResource(R.string.pinch_to_zoom)
            )
            SwitchSettingsCard(
                checked = showCharCount,
                onCheckedChange = { showCharCount = !showCharCount },
                topDp = 2.dp,
                bottomDp = 2.dp,
                text = stringResource(R.string.show_character_count_on_typing)
            )
            SwitchSettingsCard(
                checked = sendGroupAsMms,
                onCheckedChange = { sendGroupAsMms = !sendGroupAsMms },
                topDp = 2.dp,
                bottomDp = 2.dp,
                text = stringResource(R.string.send_group_messages_as_mms)
            )
            SwitchSettingsCard(
                checked = sendLongAsMms,
                onCheckedChange = { sendLongAsMms = !sendLongAsMms },
                topDp = 2.dp,
                bottomDp = 2.dp,
                text = stringResource(R.string.send_long_messages_as_mms)
            )
            SwitchSettingsCard(
                checked = enableDeliveryReports,
                onCheckedChange = { enableDeliveryReports = !enableDeliveryReports },
                topDp = 2.dp,
                bottomDp = 24.dp,
                text = stringResource(R.string.enable_delivery_reports)
            )
        }
    }
}