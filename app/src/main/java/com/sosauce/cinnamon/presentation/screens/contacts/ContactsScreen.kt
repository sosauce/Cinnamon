@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.contacts

import android.accounts.AccountManager
import android.provider.ContactsContract
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberSortContactsAscending
import com.sosauce.cinnamon.domain.model.CuteContact
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.shared_components.menus.SortingDropdownMenu
import com.sosauce.cinnamon.presentation.shared_components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.utils.CuteRoundedCornerShape
import com.sosauce.cinnamon.utils.getItemShape
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ContactsScreen(
    state: ContactsState,
    onNavigate: (Screen) -> Unit,
    onHandleContactsAction: (ContactsAction) -> Unit
) {


    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {

        var sortContactsAscending by rememberSortContactsAscending()

        Scaffold(
            bottomBar = {
                CuteSearchbar(
                    modifier = Modifier.selfAlignHorizontally(),
                    textFieldState = state.textFieldState,
                    sortingMenu = {
                        SortingDropdownMenu(
                            isSortedAscending = sortContactsAscending,
                            onChangeSorting = { sortContactsAscending = it }
                        ) {
                            state.contactAccounts.fastForEach { account ->
                                DropdownMenuItem(
                                    selected = state.accountFilter == account.type,
                                    onClick = { onHandleContactsAction(ContactsAction.ChangeAccountFiltering(account.type)) },
                                    shapes = MenuDefaults.itemShapes(),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.contact),
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text(account.name) }
                                )
                            }
                        }
                    },
                    fab = {
                        FloatingActionButton(
                            onClick = { onNavigate(Screen.ContactEditor(CuteContact())) },
                            shape = MaterialShapes.Cookie9Sided.toShape()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null
                            )
                        }
                    },
                    onNavigate = onNavigate
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues
            ) {
                groupedContactsList(
                    contacts = state.contacts,
                    onContactClicked = { onNavigate(Screen.ContactDetails(it.id)) }
                )
            }
        }
    }
}

fun LazyListScope.groupedContactsList(
    contacts: List<CuteContact>,
    onContactClicked: (CuteContact) -> Unit,
    showPhoneNumbers: Boolean = false
) {
    contacts.groupBy {
        if (it.isFavorite) '*' else (it.displayName.firstOrNull()?.uppercaseChar() ?: '#')
    }.toSortedMap().forEach { (letter, contacts) ->
        item {
            if (letter == '*') {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.favorite_filled),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = pluralStringResource(R.plurals.favorites, contacts.size),
                        style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            } else {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
        items(
            items = contacts,
            key = { contact -> contact.id }
        ) { contact ->
            ContactListItem(
                modifier = Modifier.animateItem(),
                contact = contact,
                onContactClick = { onContactClicked(contact) },
                showNumber = showPhoneNumbers
            )
        }
    }
}