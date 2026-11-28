@file:OptIn(ExperimentalMaterial3Api::class)

package com.sosauce.cinnamon.settings

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.copyMutate
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.features.contacts.presentation.components.ContactGroupsSheet
import com.sosauce.nekobites.components.settings.SettingsClickable
import com.sosauce.nekobites.components.settings.SettingsGroup
import com.sosauce.nekobites.helpers.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsContacts() {

    val context = LocalContext.current
    val resources = LocalResources.current
    val labelsViewModel = koinViewModel<ContactLabelsViewModel>()
    val labelsState by labelsViewModel.state.collectAsStateWithLifecycle()
    var showLabelsSheet by remember { mutableStateOf(false) }

    ObserveAsEvents(labelsViewModel.events) { event ->
        when (event) {
            is ContactLabelsEvent.Error -> Toast.makeText(
                context,
                resources.getString(R.string.label_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (showLabelsSheet) {
        ContactGroupsSheet(
            groups = labelsState.groups,
            accounts = labelsState.accounts,
            memberIds = null,
            onDismissRequest = { showLabelsSheet = false },
            onToggle = {},
            onCreate = { title, account ->
                labelsViewModel.handleContactLabelsAction(ContactLabelsAction.Create(title, account))
            },
            onRename = { group, title ->
                labelsViewModel.handleContactLabelsAction(ContactLabelsAction.Rename(group, title))
            },
            onDelete = { group ->
                labelsViewModel.handleContactLabelsAction(ContactLabelsAction.Delete(group))
            }
        )
    }


    SettingsGroup(title = stringResource(R.string.contacts)) {
        SettingsClickable(
            title = stringResource(R.string.manage_labels),
            icon = R.drawable.label,
            onClick = { showLabelsSheet = true }
        )
    }
}
