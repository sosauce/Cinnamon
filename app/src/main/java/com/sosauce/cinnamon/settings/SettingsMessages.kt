package com.sosauce.cinnamon.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberDefaultMessagesSim
import com.sosauce.cinnamon.core.datastore.rememberEnableDeliveryReports
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


    Column {
        SettingsWithTitle(title = R.string.messages) {
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
                checked = showCharCount,
                onCheckedChange = { showCharCount = !showCharCount },
                topDp = 4.dp,
                bottomDp = 4.dp,
                text = "Show character count on typing"
            )
            SwitchSettingsCard(
                checked = sendGroupAsMms,
                onCheckedChange = { sendGroupAsMms = !sendGroupAsMms },
                topDp = 4.dp,
                bottomDp = 4.dp,
                text = "Send group messages as MMS"
            )
            SwitchSettingsCard(
                checked = sendLongAsMms,
                onCheckedChange = { sendLongAsMms = !sendLongAsMms },
                topDp = 4.dp,
                bottomDp = 4.dp,
                text = "Send long messages as MMS"
            )
            SwitchSettingsCard(
                checked = enableDeliveryReports,
                onCheckedChange = { enableDeliveryReports = !enableDeliveryReports },
                topDp = 4.dp,
                bottomDp = 24.dp,
                text = "Enable delivery reports"
            )
        }
    }
}