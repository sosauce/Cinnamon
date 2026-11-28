@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.datastore.rememberSortContactsAscending
import com.sosauce.cinnamon.core.ui.components.ContactsSelectedBar
import com.sosauce.cinnamon.core.ui.components.menus.SortingDropdownMenu
import com.sosauce.cinnamon.core.ui.components.searchbars.MainSearchbar
import com.sosauce.cinnamon.core.utils.LazyListKeys
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.presentation.components.FavoriteContact
import com.sosauce.cinnamon.setup.SetupDefaultDialerScreen
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.sweetselect.rememberSweetSelectState
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun SharedTransitionScope.ContactsScreen(
    state: ContactsState,
    contacts: LazyPagingItems<ContactsListItem>,
    textFieldState: TextFieldState,
    onNavigate: (Screen) -> Unit,
    onHandleContactsAction: (ContactsAction) -> Unit
) {

    var sortContactsAscending by rememberSortContactsAscending()
    val sweetSelectState = rememberSweetSelectState<CuteContact>()
    val isDefaultSmsApp by PermissionUtils.observeHasDefaultApp(PermissionUtils.SMS)
    val isDefaultDialer by PermissionUtils.observeHasDefaultApp(PermissionUtils.DIALER)
    // being either one is enough to access contacts
    val isDefaultApp = isDefaultSmsApp || isDefaultDialer

    LaunchedEffect(isDefaultApp) {
        onHandleContactsAction(ContactsAction.DefaultAppChanged(isDefaultApp))
    }

    Scaffold(
        bottomBar = {
            MainSearchbar(
                textFieldState = textFieldState,
                isInSelectionMode = sweetSelectState.isInSelectionMode,
                sortingMenu = {
                    SortingDropdownMenu(
                        isSortedAscending = sortContactsAscending,
                        onChangeSorting = { sortContactsAscending = it },
                        topContent = {
                            val labelsCount = state.labelsToCount.size + 1

                            SelectableDropdownMenuItem(
                                selected = state.labelFilter == null,
                                onClick = {
                                    onHandleContactsAction(ContactsAction.ChangeLabelFiltering(null))
                                },
                                shapes = MenuDefaults.itemShape(0, labelsCount),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.label),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(R.string.all)) }
                            )
                            state.labelsToCount.onEachIndexed { index, (label, count) ->
                                SelectableDropdownMenuItem(
                                    selected = state.labelFilter == label,
                                    onClick = {
                                        onHandleContactsAction(ContactsAction.ChangeLabelFiltering(label))
                                    },
                                    shapes = MenuDefaults.itemShape(index + 1, labelsCount),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.label),
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text(label) },
                                    trailingContent = { Text(count.toString()) }
                                )
                            }
                        }
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
                    AnimatedVisibility(
                        visible = isDefaultApp,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        AnimatedFab(
                            onClick = { onNavigate(Screen.ContactEditor(null)) },
                            icon = R.drawable.add
                        )
                    }
                },
                selectedBar = {
                    ContactsSelectedBar(
                        modifier = Modifier.selfAlignHorizontally(),
                        items = state.favorites + contacts.loadedContacts(),
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
                },
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->

        if (!isDefaultApp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SetupDefaultDialerScreen(
                    onNext = {},
                    description = R.string.contacts_desc
                )
            }
            return@Scaffold
        }

        LoadingBox(
            isLoading = state.isLoading
        ) {
            LazyColumn(
                contentPadding = paddingValues
            ) {
                // selects the contact while in selection mode, opens it otherwise
                val onContactClick: (CuteContact) -> Unit = { contact ->
                    if (sweetSelectState.isInSelectionMode) {
                        sweetSelectState.toggle(contact)
                    } else {
                        onNavigate(Screen.ContactDetails(contact.id))
                    }
                }

                contactsList(
                    favorites = state.favorites,
                    contacts = contacts,
                    emptyState = {
                        NoXFound(
                            headlineText = R.string.no_contacts_found,
                            bodyText = R.string.no_contacts_found_desc,
                            icon = R.drawable.contacts
                        )
                    },
                    favoriteContactItem = { contact ->

                        val isSelected by sweetSelectState.isSelectedAsState(contact)

                        FavoriteContact(
                            modifier = Modifier.animateItem(),
                            contact = contact,
                            isSelected = isSelected,
                            onClick = { onContactClick(contact) },
                            onLongClick = { sweetSelectState.toggle(contact) },
                            showNumber = false
                        )
                    }
                ) { contact ->

                    val isSelected by sweetSelectState.isSelectedAsState(contact)

                    ContactListItem(
                        modifier = Modifier.animateItem(),
                        contact = contact,
                        isSelected = isSelected,
                        onClick = { onContactClick(contact) },
                        onLongClick = { sweetSelectState.toggle(contact) },
                        showNumber = false
                    )
                }
            }
        }
    }
}

fun LazyListScope.contactsList(
    favorites: List<CuteContact>,
    contacts: LazyPagingItems<ContactsListItem>,
    emptyState: @Composable LazyItemScope.() -> Unit,
    favoriteContactItem: @Composable LazyItemScope.(CuteContact) -> Unit,
    contactItem: @Composable LazyItemScope.(CuteContact) -> Unit,
) {
    if (favorites.isNotEmpty()) {
        item(LazyListKeys.FAVORITE_CONTACTS) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            ) {
                items(
                    items = favorites,
                    key = { it.id }
                ) { contact -> favoriteContactItem(contact) }
            }
        }
    }

    items(
        count = contacts.itemCount,
        key = contacts.itemKey { it.key },
        contentType = contacts.itemContentType { it::class }
    ) { index ->
        when (val item = contacts[index]) {
            is ContactsListItem.Header -> {
                Text(
                    text = item.letter.toString(),
                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .animateItem()
                )
            }

            is ContactsListItem.Contact -> contactItem(item.contact)
            null -> Unit
        }
    }

    if (favorites.isEmpty() && contacts.itemCount == 0 && contacts.loadState.refresh is LoadState.NotLoading) {
        item(LazyListKeys.EMPTY_STATE) {
            Box(
                modifier = Modifier.animateItem()
            ) { emptyState() }
        }
    }
}

/**
 * Contacts loaded so far in the paged list
 */
fun LazyPagingItems<ContactsListItem>.loadedContacts(): List<CuteContact> =
    itemSnapshotList.items.mapNotNull { (it as? ContactsListItem.Contact)?.contact }

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
