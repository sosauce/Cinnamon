@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.settings

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.screens.settings.components.PlainSettingsCard
import com.sosauce.cinnamon.presentation.screens.settings.components.SettingsWithTitle
import com.sosauce.cinnamon.presentation.shared_components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.utils.copyMutate
import com.sosauce.cinnamon.utils.getItemShape
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsMigration() {


    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val viewModel = koinViewModel<MigrationViewModel>()
    val accountManager = remember(context) { context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager }
    var importSource by remember { mutableStateOf("") }
    var showContactsImportSheet by remember { mutableStateOf(false) }
    val vcfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        viewModel.handleMigrationAction(
            MigrationAction.ImportContacts(
                source = importSource,
                vCard = uri
            )
        )
    }

    if (showContactsImportSheet) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = { showContactsImportSheet = false }
        ) {

            val accounts = accountManager.accounts.toList().copyMutate { add(Account("Device", "device")) }

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
                            importSource = account.type
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


    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
        SettingsWithTitle(
            title = R.string.contacts
        ) {
            PlainSettingsCard(
                onClick = { showContactsImportSheet = true },
                topDp = 24.dp,
                bottomDp = 4.dp,
                text = "Import contacts from a .vcf file"
            )
            PlainSettingsCard(
                onClick = {},
                topDp = 4.dp,
                bottomDp = 24.dp,
                text = "Export contacts to a .vcf file"
            )
        }
    }

}