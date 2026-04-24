@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberDefaultMessagesSim
import com.sosauce.cinnamon.data.datastore.rememberDefaultTab
import com.sosauce.cinnamon.data.datastore.rememberEnableDeliveryReports
import com.sosauce.cinnamon.data.datastore.rememberSendGroupAsMms
import com.sosauce.cinnamon.data.datastore.rememberSendLongAsMms
import com.sosauce.cinnamon.presentation.screens.settings.components.DropdownMenuSettingsCard
import com.sosauce.cinnamon.presentation.screens.settings.components.LazyRowWithScrollButton
import com.sosauce.cinnamon.presentation.screens.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.presentation.screens.settings.components.SimSelector
import com.sosauce.cinnamon.presentation.screens.settings.components.SwitchSettingsCards
import com.sosauce.cinnamon.presentation.shared_components.SimsViewModel
import com.sosauce.cinnamon.presentation.shared_components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.utils.DefaultTabOption
import com.sosauce.cinnamon.utils.getItemShape
import com.sosauce.cinnamon.utils.toLocalizedTab
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsBehavior() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var sendGroupAsMms by rememberSendGroupAsMms()
    var enableDeliveryReports by rememberEnableDeliveryReports()
    var sendLongAsMms by rememberSendLongAsMms()
    val simsViewModel = koinViewModel<SimsViewModel>()
    var defaultMessagesSims by rememberDefaultMessagesSim()
    var defaultTab by rememberDefaultTab()
    val allTabs = listOf(
        DefaultTabOption.MESSAGES,
        DefaultTabOption.CONTACTS,
        DefaultTabOption.DIALER,
        DefaultTabOption.DIALPAD
    )


    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
        SettingsWithTitle(title = R.string.general) {
            DropdownMenuSettingsCard(
                value = context.toLocalizedTab(defaultTab),
                topDp = 24.dp,
                bottomDp = 24.dp,
                text = R.string.default_tab,
                dropdownContent = {
                    allTabs.fastForEachIndexed { index, tab ->
                        DropdownMenuItem(
                            onClick = { defaultTab = tab },
                            text = { Text(context.toLocalizedTab(tab)) },
                            leadingIcon = {
                                RadioButton(
                                    selected = tab == defaultTab,
                                    onClick = null
                                )
                            },
                            shape = MenuDefaults.getItemShape(index, allTabs.lastIndex)
                        )
                    }
                }
            )
        }
        SettingsWithTitle(title = R.string.messages) {
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
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
            SwitchSettingsCards(
                checked = sendGroupAsMms,
                onCheckedChange = { sendGroupAsMms = !sendGroupAsMms },
                topDp = 4.dp,
                bottomDp = 4.dp,
                text = "Send group messages as MMS"
            )
            SwitchSettingsCards(
                checked = sendLongAsMms,
                onCheckedChange = { sendLongAsMms = !sendLongAsMms },
                topDp = 4.dp,
                bottomDp = 4.dp,
                text = "Send long messages as MMS"
            )
            SwitchSettingsCards(
                checked = enableDeliveryReports,
                onCheckedChange = { enableDeliveryReports = !enableDeliveryReports },
                topDp = 4.dp,
                bottomDp = 24.dp,
                text = "Enable delivery reports"
            )
        }
    }
}