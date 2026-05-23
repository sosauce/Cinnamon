package com.sosauce.cinnamon.presentation.screens.settings

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberGroupSubsequentCalls
import com.sosauce.cinnamon.presentation.screens.settings.components.LazyRowWithScrollButton
import com.sosauce.cinnamon.presentation.screens.settings.components.PhoneAccountHandleSelector
import com.sosauce.cinnamon.presentation.screens.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.presentation.screens.settings.components.SwitchSettingsCard
import com.sosauce.cinnamon.presentation.shared_components.SimsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsPhone() {

    val scrollState = rememberScrollState()
    var groupSubCalls by rememberGroupSubsequentCalls()
    val simsViewModel = koinViewModel<SimsViewModel>()
    val allHandles = simsViewModel.fetchPhoneHandles()
    val defaultPhoneHandle by simsViewModel.fetchLatestDefaultPhoneHandle()
        .collectAsStateWithLifecycle(null)





    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {

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
                LazyRowWithScrollButton(
                    items = allHandles
                ) { (account, handle) ->
                    PhoneAccountHandleSelector(
                        account = account,
                        isDefaultHandle = handle == defaultPhoneHandle,
                        onClick = { simsViewModel.saveDefaultPhoneHandle(handle) }
                    )
                }
                Text(
                    text = "Default sim for phone functions.",
                    style = MaterialTheme.typography.bodySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(5.dp)
                )
            }
            SwitchSettingsCard(
                checked = groupSubCalls,
                onCheckedChange = { groupSubCalls = !groupSubCalls },
                topDp = 4.dp,
                bottomDp = 24.dp,
                text = "Group subsequent calls"
            )
        }
    }
}