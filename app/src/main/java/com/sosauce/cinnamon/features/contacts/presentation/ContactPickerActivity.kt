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
import androidx.paging.compose.collectAsLazyPagingItems
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.applock.AppLockGate
import com.sosauce.cinnamon.core.applock.AppLockManager
import com.sosauce.cinnamon.core.applock.bindAppLockSecureFlag
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.features.contacts.presentation.components.FavoriteContact
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class ContactPickerActivity : ComponentActivity() {

    private val appLockManager by inject<AppLockManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindAppLockSecureFlag(appLockManager)


        setContent {
            CinnamonTheme {
                AppLockGate(onExit = ::finish) {

                    val contactsViewModel = koinViewModel<ContactsViewModel>()
                    val state by contactsViewModel.state.collectAsStateWithLifecycle()
                    val contacts = contactsViewModel.contacts.collectAsLazyPagingItems()


                    SharedTransitionLayout {
                        LoadingBox(
                            isLoading = state.isLoading
                        ) {
                            Scaffold { paddingValues ->
                                LazyColumn(
                                    contentPadding = paddingValues
                                ) {
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


                                            FavoriteContact(
                                                modifier = Modifier.animateItem(),
                                                contact = contact,
                                                isSelected = false,
                                                onClick = {},
                                                onLongClick = { },
                                                showNumber = false
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


    }

    override fun onStart() {
        appLockManager.onActivityStarted()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        appLockManager.onActivityResumed()
    }

    override fun onStop() {
        super.onStop()
        appLockManager.onActivityStopped(isChangingConfigurations)
    }


}
