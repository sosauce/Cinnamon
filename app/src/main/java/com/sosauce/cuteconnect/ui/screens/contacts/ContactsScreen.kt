@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sosauce.cuteconnect.domain.model.CuteContact
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.ui.screens.contacts.components.AboutMeCard
import com.sosauce.cuteconnect.ui.shared_components.searchbars.CuteSearchbar
import com.sosauce.cuteconnect.utils.rememberSearchbarAlignment
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ContactsScreen(
    contacts: List<CuteContact>,
    onNavigate: (Screen) -> Unit,
) {

    val textFieldState = rememberTextFieldState()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues
            ) {
                item(
                    key = "MeCard"
                ) {
                    AboutMeCard(
                        onNavigate = onNavigate
                    )
                }
                items(
                    items = contacts,
                    key = { it.id }
                ) { contact ->
                    ContactListItem(
                        contact = contact,
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 4.dp),
                        onContactClick = { onNavigate(Screen.ContactDetails(contact.id)) }
                    )
                }
            }
        }

        CuteSearchbar(
            modifier = Modifier.align(rememberSearchbarAlignment()),
            textFieldState = textFieldState,
            sortingMenu = {},
            fab = {
                SmallFloatingActionButton(
                    onClick = {}
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                }
            },
            onNavigate = onNavigate
        )
    }
}