package com.sosauce.cinnamon.settings

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberDefaultMessagesSim
import com.sosauce.cinnamon.core.datastore.rememberEnableDeliveryReports
import com.sosauce.cinnamon.core.datastore.rememberEnablePinchToZoom
import com.sosauce.cinnamon.core.datastore.rememberSendGroupAsMms
import com.sosauce.cinnamon.core.datastore.rememberSendLongAsMms
import com.sosauce.cinnamon.core.datastore.rememberShowCharCount
import com.sosauce.cinnamon.settings.components.SettingsEmptyState
import com.sosauce.cinnamon.settings.components.SettingsGroup
import com.sosauce.cinnamon.settings.components.SettingsPicker
import com.sosauce.cinnamon.settings.components.SettingsSwitch
import com.sosauce.cinnamon.settings.components.SimSelector
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
    val sims = remember { simsViewModel.fetchSims() }

    SettingsGroup(title = stringResource(R.string.sim_card)) {
        if (sims.isEmpty()) {
            SettingsEmptyState(
                icon = R.drawable.sim_card,
                text = stringResource(R.string.no_sim_available)
            )
        } else {
            SettingsPicker(
                title = stringResource(R.string.default_sim),
                description = stringResource(R.string.default_sim_messages)
            ) {
                items(sims) { sim ->
                    SimSelector(
                        simCard = sim,
                        isDefaultSim = defaultMessagesSims == sim.subId,
                        onClick = { defaultMessagesSims = sim.subId }
                    )
                }
            }
        }
    }

    SettingsGroup(title = stringResource(R.string.conversations)) {
        SettingsSwitch(
            title = stringResource(R.string.pinch_to_zoom),
            checked = pinchToZoom,
            onCheckedChange = { pinchToZoom = !pinchToZoom }
        )
        SettingsSwitch(
            title = stringResource(R.string.show_character_count_on_typing),
            checked = showCharCount,
            onCheckedChange = { showCharCount = !showCharCount }
        )
    }

    SettingsGroup(title = stringResource(R.string.sending_settings)) {
        SettingsSwitch(
            title = stringResource(R.string.send_group_messages_as_mms),
            checked = sendGroupAsMms,
            onCheckedChange = { sendGroupAsMms = !sendGroupAsMms }
        )
        SettingsSwitch(
            title = stringResource(R.string.send_long_messages_as_mms),
            checked = sendLongAsMms,
            onCheckedChange = { sendLongAsMms = !sendLongAsMms }
        )
        SettingsSwitch(
            title = stringResource(R.string.enable_delivery_reports),
            checked = enableDeliveryReports,
            onCheckedChange = { enableDeliveryReports = !enableDeliveryReports }
        )
    }
}
