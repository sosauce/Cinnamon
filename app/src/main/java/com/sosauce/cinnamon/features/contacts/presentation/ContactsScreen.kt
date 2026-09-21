@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.datastore.rememberSortContactsAscending
import com.sosauce.cinnamon.core.ui.components.ContactsSelectedBar
import com.sosauce.cinnamon.core.ui.components.menus.SortingDropdownMenu
import com.sosauce.cinnamon.core.ui.components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.core.utils.LazyListKeys
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.sweetselect.rememberSweetSelectState
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun SharedTransitionScope.ContactsScreen(
    state: ContactsState,
    textFieldState: TextFieldState,
    onNavigate: (Screen) -> Unit,
    onHandleContactsAction: (ContactsAction) -> Unit
) {

    var sortContactsAscending by rememberSortContactsAscending()
    val sweetSelectState = rememberSweetSelectState<CuteContact>()




    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {


        Scaffold(
            bottomBar = {
                AnimatedContent(
                    targetState = sweetSelectState.isInSelectionMode,
                ) {
                    if (it) {
                        ContactsSelectedBar(
                            modifier = Modifier.selfAlignHorizontally(),
                            items = state.contacts,
                            multiSelectState = sweetSelectState,
                            onDeleteContacts = {
                                val ids = sweetSelectState.selectedItems.map { it.id }
                                onHandleContactsAction(ContactsAction.DeleteContacts(ids))
                                sweetSelectState.clearSelected()
                            },
                            onToggleFavorite = {
                                val contacts = sweetSelectState.selectedItems.toList()
                                onHandleContactsAction(ContactsAction.ToggleFavorite(contacts))
                                sweetSelectState.clearSelected()
                            }
                        )
                    } else {
                        CuteSearchbar(
                            modifier = Modifier.selfAlignHorizontally(),
                            textFieldState = textFieldState,
                            sortingMenu = {
                                SortingDropdownMenu(
                                    isSortedAscending = sortContactsAscending,
                                    onChangeSorting = { sortContactsAscending = it }
                                ) {
                                    state.accountsToCount.onEachIndexed { index, (account, count) ->
                                        SelectableDropdownMenuItem(
                                            selected = state.accountFilter == account,
                                            onClick = {
                                                onHandleContactsAction(
                                                    ContactsAction.ChangeAccountFiltering(
                                                        account
                                                    )
                                                )
                                            },
                                            shapes = MenuDefaults.itemShape(
                                                index,
                                                state.accountsToCount.size
                                            ),
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.contact),
                                                    contentDescription = null
                                                )
                                            },
                                            text = { Text(account) },
                                            trailingContent = { Text(count.toString()) }
                                        )
                                    }
                                }
                            },
                            fab = {
                                AnimatedFab(
                                    onClick = { onNavigate(Screen.ContactEditor(null)) },
                                    icon = R.drawable.add
                                )
                            },
                            onNavigate = onNavigate
                        )

                    }
                }
            }
        ) { paddingValues ->

            LoadingBox(
                isLoading = state.isLoading
            ) {
                LazyColumn(
                    contentPadding = paddingValues
                ) {
                    if (state.contacts.isNotEmpty()) {

                        val (favorites, nonFavorites) = state.contacts.partition { it.isFavorite }

                        if (favorites.isNotEmpty()) {
                            item(LazyListKeys.FAVORITE_CONTACTS) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.favorite_filled),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.favorites,
                                            favorites.size
                                        ),
                                        style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                            items(
                                items = favorites,
                                key = { contact -> contact.id }
                            ) { contact ->

                                val isSelected by sweetSelectState.isSelectedAsState(contact)


                                ContactListItem(
                                    modifier = Modifier.animateItem(),
                                    contact = contact,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (sweetSelectState.isInSelectionMode) {
                                            sweetSelectState.toggle(contact)
                                        } else {
                                            onNavigate(Screen.ContactDetails(contact.id))
                                        }
                                    },
                                    onLongClick = { sweetSelectState.toggle(contact) },
                                    showNumber = false
                                )
                            }
                        }


                        nonFavorites.groupBy { it.displayName.firstOrNull()?.uppercaseChar() ?: '#' }
                            .forEach { (letter, contacts) ->
                                item(
                                    key = letter
                                ) {
                                    Text(
                                        text = letter.toString(),
                                        style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .padding(
                                                horizontal = 20.dp,
                                                vertical = 10.dp
                                            )
                                            .animateItem()
                                    )
                                }
                                items(
                                    items = contacts,
                                    key = { contact -> contact.id }
                                ) { contact ->

                                    val isSelected by sweetSelectState.isSelectedAsState(contact)

                                    ContactListItem(
                                        modifier = Modifier.animateItem(),
                                        contact = contact,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (sweetSelectState.isInSelectionMode) {
                                                sweetSelectState.toggle(contact)
                                            } else {
                                                onNavigate(Screen.ContactDetails(contact.id))
                                            }
                                        },
                                        onLongClick = { sweetSelectState.toggle(contact) },
                                        showNumber = false
                                    )
                                }
                            }
                    } else {
                        item {
                            NoXFound(
                                headlineText = R.string.no_contacts_found,
                                bodyText = R.string.no_contacts_found_desc,
                                icon = R.drawable.contacts
                            )
                        }
                    }
                }
            }

        }
    }
}

//fun LazyListScope.groupedContactsList(
//    contacts: List<CuteContact>,
//    onContactClicked: (CuteContact) -> Unit,
//    showPhoneNumbers: Boolean = false,
//    sharedTransitionScope: SharedTransitionScope,
//    emptyState: @Composable () -> Unit
//) {
//
//    if (contacts.isNotEmpty()) {
//
//        val (favorites, nonFavorites) = contacts.partition { it.isFavorite }
//
//        if (favorites.isNotEmpty()) {
//            item(LazyListKeys.FAVORITE_CONTACTS) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.favorite_filled),
//                        contentDescription = null,
//                        modifier = Modifier.size(18.dp),
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                    Spacer(Modifier.width(5.dp))
//                    Text(
//                        text = pluralStringResource(R.plurals.favorites, favorites.size),
//                        style = MaterialTheme.typography.bodyLargeEmphasized.copy(
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    )
//                }
//            }
//            with(sharedTransitionScope) {
//                items(
//                    items = favorites,
//                    key = { contact -> contact.id }
//                ) { contact ->
//                    ContactListItem(
//                        modifier = Modifier.animateItem(),
//                        contact = contact,
//                        onClick = { onContactClicked(contact) },
//                        showNumber = showPhoneNumbers
//                    )
//                }
//            }
//        }
//
//
//        nonFavorites.groupBy { it.displayName.firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap().forEach { (letter, contacts) ->
//            item {
//                Text(
//                    text = letter.toString(),
//                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
//                        color = MaterialTheme.colorScheme.primary
//                    ),
//                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
//                )
//            }
//            with(sharedTransitionScope) {
//                items(
//                    items = contacts,
//                    key = { contact -> contact.id }
//                ) { contact ->
//                    ContactListItem(
//                        modifier = Modifier.animateItem(),
//                        contact = contact,
//                        onClick = { onContactClicked(contact) },
//                        showNumber = showPhoneNumbers
//                    )
//                }
//            }
//        }
//    } else {
//        item { emptyState() }
//    }
//
//}