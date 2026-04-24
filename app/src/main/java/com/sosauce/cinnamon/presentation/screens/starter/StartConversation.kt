@file:OptIn(ExperimentalMaterial3Api::class)

package com.sosauce.cinnamon.presentation.screens.starter

import android.provider.Telephony
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.contacts.ContactListItem
import com.sosauce.cinnamon.presentation.screens.contacts.components.dialogs.NumberPickerDialog
import com.sosauce.cinnamon.presentation.screens.contacts.groupedContactsList
import com.sosauce.cinnamon.presentation.shared_components.searchbars.MiniCuteSearchbar
import com.sosauce.cinnamon.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.utils.selfAlignHorizontally

@Composable
fun StartConversation(
    state: StartConversationState,
    onNavigateUp: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    var contactPhoneNumbersPicker by remember { mutableStateOf<List<String>?>(null) }

    if (contactPhoneNumbersPicker != null) {
        NumberPickerDialog(
            onDismissRequest = { contactPhoneNumbersPicker = null },
            onPickNumber = { number ->
                contactPhoneNumbersPicker = null
                onNavigate(Screen.Conversation(number.getThreadIdOrCreate(context)))
            },
            phoneNumbers = contactPhoneNumbersPicker!!
        )
    }




    Scaffold(
        bottomBar = {
            MiniCuteSearchbar(
                modifier = Modifier.selfAlignHorizontally(),
                textFieldState = state.textFieldState,
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues
        ) {
            groupedContactsList(
                contacts = state.contacts,
                showPhoneNumbers = true,
                onContactClicked = { contact ->
                    if (contact.details.phoneNumbers.size > 1) {
                        contactPhoneNumbersPicker = contact.details.phoneNumbers.fastMap { it.number }
                    } else {
                        val threadId = (contact.details.phoneNumbers.firstOrNull() ?: return@groupedContactsList).number.getThreadIdOrCreate(context)
                        onNavigate(Screen.Conversation(threadId))
                    }
                }
            )
        }
    }
}