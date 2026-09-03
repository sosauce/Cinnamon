package com.sosauce.cinnamon.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberEnableT9Dialing
import com.sosauce.cinnamon.core.datastore.rememberGroupSubsequentCalls
import com.sosauce.cinnamon.settings.components.PhoneAccountHandleSelector
import com.sosauce.cinnamon.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.settings.components.SwitchSettingsCard
import com.sosauce.nekobites.components.LazyRowWithScrollButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsPhone() {

    var groupSubCalls by rememberGroupSubsequentCalls()
    var t9Dialing by rememberEnableT9Dialing()
    val simsViewModel = koinViewModel<SimsViewModel>()
    val allHandles = try {
        simsViewModel.fetchPhoneHandles()
    } catch (_: Exception) {
        emptyMap()
    }
    val defaultPhoneHandle by simsViewModel.fetchLatestDefaultPhoneHandle().collectAsStateWithLifecycle(null)

    Column {

        SettingsWithTitle(title = R.string.phone) {
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomEnd = 4.dp,
                    bottomStart = 4.dp
                )
            ) {

                if (allHandles.isEmpty()) {
                    Text(
                        text = "No SIM available or permission needed — grant Phone permission to choose default SIM",
                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    LazyRow {
                        items(
                            items = allHandles.keys.toList()
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
                Text(
                    text = stringResource(R.string.default_sim_phone),
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(5.dp)
                )
            }
            SwitchSettingsCard(
                checked = t9Dialing,
                onCheckedChange = { t9Dialing = !t9Dialing },
                topDp = 4.dp,
                bottomDp = 4.dp,
                text = stringResource(R.string.enable_t9_dialing)
            )
            SwitchSettingsCard(
                checked = groupSubCalls,
                onCheckedChange = { groupSubCalls = !groupSubCalls },
                topDp = 4.dp,
                bottomDp = 24.dp,
                text = stringResource(R.string.group_sub_calls)
            )
        }
    }
}