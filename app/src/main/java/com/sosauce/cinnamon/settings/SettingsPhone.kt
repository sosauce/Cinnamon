package com.sosauce.cinnamon.settings

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberEnableT9Dialing
import com.sosauce.cinnamon.core.datastore.rememberGroupSubsequentCalls
import com.sosauce.cinnamon.settings.components.PhoneAccountHandleSelector
import com.sosauce.nekobites.components.settings.SettingsEmptyState
import com.sosauce.nekobites.components.settings.SettingsGroup
import com.sosauce.nekobites.components.settings.SettingsPicker
import com.sosauce.nekobites.components.settings.SettingsSwitch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsPhone() {

    var groupSubCalls by rememberGroupSubsequentCalls()
    var t9Dialing by rememberEnableT9Dialing()
    val simsViewModel = koinViewModel<SimsViewModel>()
    val allHandles = remember(simsViewModel) {
        try {
            simsViewModel.fetchPhoneHandles()
        } catch (_: Exception) {
            emptyMap()
        }
    }
    val defaultPhoneHandle by simsViewModel.fetchLatestDefaultPhoneHandle().collectAsStateWithLifecycle(null)

    SettingsGroup(title = stringResource(R.string.sim_card)) {
        if (allHandles.isEmpty()) {
            SettingsEmptyState(
                icon = R.drawable.sim_card,
                text = stringResource(R.string.no_sim_phone_permission)
            )
        } else {
            SettingsPicker(
                title = stringResource(R.string.default_sim),
                description = stringResource(R.string.default_sim_phone)
            ) {
                items(
                    items = allHandles.keys.toList(),
                    key = { it.accountHandle }
                ) { account ->
                    val handle = allHandles[account] ?: return@items
                    PhoneAccountHandleSelector(
                        account = account,
                        isDefaultHandle = handle == defaultPhoneHandle,
                        onClick = { simsViewModel.saveDefaultPhoneHandle(handle) }
                    )
                }
            }
        }
    }

    SettingsGroup(title = stringResource(R.string.phone)) {
        SettingsSwitch(
            title = stringResource(R.string.enable_t9_dialing),
            checked = t9Dialing,
            onCheckedChange = { t9Dialing = !t9Dialing }
        )
        SettingsSwitch(
            title = stringResource(R.string.group_sub_calls),
            checked = groupSubCalls,
            onCheckedChange = { groupSubCalls = !groupSubCalls }
        )
    }
}
