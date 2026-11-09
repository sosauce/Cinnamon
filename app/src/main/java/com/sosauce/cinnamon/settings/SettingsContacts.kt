@file:OptIn(ExperimentalMaterial3Api::class)

package com.sosauce.cinnamon.settings

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.copyMutate
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.settings.components.SettingsClickable
import com.sosauce.cinnamon.settings.components.SettingsGroup
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsContacts() {

    val context = LocalContext.current
    val backupViewModel = koinViewModel<BackupViewModel>()
    var importSource by remember { mutableStateOf("" to "") }
    val accountManager =
        remember(context) { context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager }
    var showContactsImportSheet by remember { mutableStateOf(false) }

    val vcfPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            backupViewModel.handleMigrationAction(
                MigrationAction.ImportContacts(
                    importSource,
                    uri
                )
            )

        }

    if (showContactsImportSheet) {
        ModalBottomSheet(
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden
            ),
            onDismissRequest = { showContactsImportSheet = false }
        ) {

            val accounts =
                accountManager.accounts.toList().copyMutate { add(Account("Device", "device")) }

            Column {
                Text(
                    text = "Choose where to save imported contacts",
                    style = MaterialTheme.typography.titleSmallEmphasized.copy(
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
                accounts.fastForEachIndexed { index, account ->
                    DropdownMenuItem(
                        onClick = {
                            importSource = account.type to account.name
                            vcfPicker.launch(arrayOf("text/vcard", "text/x-vcard"))
                            showContactsImportSheet = false
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.mobile),
                                contentDescription = null,
                                modifier = Modifier.alpha(if (account.type == "device") 1f else 0f)
                            )
                        },
                        text = {
                            Column {
                                Text(account.name)

                                if (account.type == "device") {
                                    Text(
                                        text = "Contacts may not sync or be available on other devices",
                                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                            }
                        },
                        shape = MenuDefaults.getItemShape(index, accounts.lastIndex)
                    )
                }
            }
        }
    }


    SettingsGroup(title = stringResource(R.string.contacts)) {
        SettingsClickable(
            title = stringResource(R.string.import_contact_from_vcf),
            icon = R.drawable.contacts,
            onClick = { showContactsImportSheet = true }
        )
    }
}
