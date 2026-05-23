@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.screens.contacts.ContactListItem
import com.sosauce.cinnamon.presentation.screens.contacts.ContactsViewModel
import com.sosauce.cinnamon.presentation.shared_components.NoXFound
import com.sosauce.cinnamon.utils.LazyListKeys
import org.koin.androidx.compose.koinViewModel

class ContactPickerActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {

            val contactsViewModel = koinViewModel<ContactsViewModel>()
            val state by contactsViewModel.state.collectAsStateWithLifecycle()


            SharedTransitionLayout {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { ContainedLoadingIndicator() }
                } else {
                    Scaffold { paddingValues ->
                        LazyColumn(
                            contentPadding = paddingValues
                        ) {
                            if (state.contacts.isNotEmpty()) {

                                val (favorites, nonFavorites) = state.contacts.partition { it.isFavorite }

                                if (favorites.isNotEmpty()) {
                                    item(LazyListKeys.FAVORITE_CONTACTS) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                horizontal = 20.dp,
                                                vertical = 10.dp
                                            ),
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
                                                )
                                            )
                                        }
                                    }
                                    items(
                                        items = favorites,
                                        key = { contact -> contact.id }
                                    ) { contact ->


                                        ContactListItem(
                                            modifier = Modifier.animateItem(),
                                            contact = contact,
                                            isSelected = false,
                                            onClick = {
                                                //onNavigate(Screen.ContactDetails(contact.id))
                                            },
                                            showNumber = false
                                        )
                                    }
                                }


                                nonFavorites.groupBy {
                                    it.displayName.firstOrNull()?.uppercaseChar() ?: '#'
                                }.toSortedMap().forEach { (letter, contacts) ->
                                    item {
                                        Text(
                                            text = letter.toString(),
                                            style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.padding(
                                                horizontal = 20.dp,
                                                vertical = 10.dp
                                            )
                                        )
                                    }
                                    items(
                                        items = contacts,
                                        key = { contact -> contact.id }
                                    ) { contact ->


                                        ContactListItem(
                                            modifier = Modifier.animateItem(),
                                            contact = contact,
                                            isSelected = false,
                                            onClick = {
                                                //onNavigate(Screen.ContactDetails(contact.id))
                                            },
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


    }


}