package com.sosauce.cinnamon.features.contacts.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import org.koin.androidx.compose.koinViewModel

class ContactPickerActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {

            val contactsViewModel = koinViewModel<ContactsViewModel>()
            val state by contactsViewModel.state.collectAsStateWithLifecycle()


            SharedTransitionLayout {
                LoadingBox(
                    isLoading = state.isLoading
                ) {
                    Scaffold { paddingValues ->
                        LazyColumn(
                            contentPadding = paddingValues
                        ) {
                            contactsList(
                                contacts = state.contacts,
                                emptyState = {
                                    NoXFound(
                                        headlineText = R.string.no_contacts_found,
                                        bodyText = R.string.no_contacts_found_desc,
                                        icon = R.drawable.contacts
                                    )
                                }
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
                    }
                }
            }

        }


    }


}
