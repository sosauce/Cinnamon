@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.app.navigation

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.widget.Toast
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.paging.compose.collectAsLazyPagingItems
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberInitialScreenBlocking
import com.sosauce.cinnamon.core.ui.ChatColor
import com.sosauce.cinnamon.core.ui.components.dialogs.SimPickerDialog
import com.sosauce.cinnamon.core.ui.navBackTransition
import com.sosauce.cinnamon.core.ui.navForwardTransition
import com.sosauce.cinnamon.core.utils.LocalScreen
import com.sosauce.cinnamon.core.utils.createDefaultDialerIntent
import com.sosauce.cinnamon.core.utils.createDefaultSmsIntent
import com.sosauce.cinnamon.core.utils.rememberHazeState
import com.sosauce.cinnamon.core.utils.tabToScreen
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsAction
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsEvent
import com.sosauce.cinnamon.features.contacts.presentation.ContactsEvent
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsScreen
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsViewModel
import com.sosauce.cinnamon.features.contacts.presentation.ContactsScreen
import com.sosauce.cinnamon.features.contacts.presentation.ContactsViewModel
import com.sosauce.cinnamon.features.contacts.presentation.editor.EditContactEvent
import com.sosauce.cinnamon.features.contacts.presentation.editor.EditContactScreen
import com.sosauce.cinnamon.features.contacts.presentation.editor.EditContactViewModel
import com.sosauce.cinnamon.features.messaging.presentation.archived.ArchivedConversationsScreen
import com.sosauce.cinnamon.features.messaging.presentation.archived.ArchivedConversationsViewModel
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsEvents
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsScreen
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsViewModel
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationsScreen
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationsViewModel
import com.sosauce.cinnamon.features.messaging.presentation.conversation.about.AboutConversationScreen
import com.sosauce.cinnamon.features.messaging.presentation.conversation.forwarding.ForwardingScreen
import com.sosauce.cinnamon.features.messaging.presentation.conversation.forwarding.ForwardingViewModel
import com.sosauce.cinnamon.features.messaging.presentation.customization.ConversationTheming
import com.sosauce.cinnamon.features.messaging.presentation.customization.ThemingViewModel
import com.sosauce.cinnamon.features.messaging.presentation.starter.StartConversation
import com.sosauce.cinnamon.features.messaging.presentation.starter.StartConversationViewModel
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.cinnamon.features.phone.presentation.call.CallEvents
import com.sosauce.cinnamon.features.phone.presentation.call.CallingViewModel
import com.sosauce.cinnamon.features.phone.presentation.dialpad.DialpadScreen
import com.sosauce.cinnamon.features.phone.presentation.dialpad.DialpadViewModel
import com.sosauce.cinnamon.features.phone.presentation.logs.CallLogsScreen
import com.sosauce.cinnamon.features.phone.presentation.logs.CallLogsViewModel
import com.sosauce.cinnamon.features.phone.presentation.voicemail.VoicemailScreen
import com.sosauce.cinnamon.features.phone.presentation.voicemail.VoicemailViewModel
import com.sosauce.cinnamon.settings.SettingsScreen
import com.sosauce.cinnamon.setup.launchRoleRequest
import com.sosauce.cinnamon.setup.rememberRoleRequestLauncher
import com.sosauce.nekobites.helpers.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun Nav(
    intent: Intent?
) {

    val context = LocalContext.current
    val resources = LocalResources.current
    val initialTab = rememberInitialScreenBlocking()
    val backStack = rememberNavBackStack(initialTab.tabToScreen())

    // Launched when a call/message is refused because Cinnamon isn't the default app for it
    val dialerRoleLauncher = rememberRoleRequestLauncher(onGranted = {})
    val smsRoleLauncher = rememberRoleRequestLauncher(onGranted = {})
    val requestDefaultDialer = {
        dialerRoleLauncher.launchRoleRequest(context.createDefaultDialerIntent())
    }
    val requestDefaultSmsApp = {
        smsRoleLauncher.launchRoleRequest(context.createDefaultSmsIntent())
    }

    LaunchedEffect(intent) { backStack.handleIntent(context, intent) }



    CompositionLocalProvider(
        LocalScreen provides backStack.last()
    ) {
        SharedTransitionLayout {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                transitionSpec = { navForwardTransition() },
                predictivePopTransitionSpec = { navBackTransition() },
                popTransitionSpec = { navBackTransition() },
                entryProvider = entryProvider {

                    entry<Screen.Contacts> {
                        val viewModel = koinViewModel<ContactsViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val contacts = viewModel.contacts.collectAsLazyPagingItems()

                        ObserveAsEvents(viewModel.events) { event ->
                            when (event) {
                                is ContactsEvent.DeleteError -> {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.delete_contacts_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        ContactsScreen(
                            state = state,
                            contacts = contacts,
                            textFieldState = viewModel.textFieldState,
                            onNavigate = backStack::navigate,
                            onHandleContactsAction = viewModel::handleContactsAction
                        )
                    }

                    entry<Screen.ContactDetails> { key ->

                        val viewModel = koinViewModel<ContactDetailsViewModel>(
                            parameters = { parametersOf(key.contactId) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()


                        ObserveAsEvents(viewModel.events) { event ->
                            when (event) {
                                is ContactDetailsEvent.Error -> {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.delete_contact_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                is ContactDetailsEvent.Success -> backStack.navigateBack()
                                is ContactDetailsEvent.RequestDefaultDialer -> requestDefaultDialer()
                            }
                        }

                        val simPicker by viewModel.simPicker.collectAsStateWithLifecycle()
                        simPicker?.let { picker ->
                            SimPickerDialog(
                                state = picker,
                                onPickSim = { viewModel.handleContactDetailsAction(ContactDetailsAction.PickSim(it)) },
                                onDismissRequest = { viewModel.handleContactDetailsAction(ContactDetailsAction.DismissSimPicker) }
                            )
                        }

                        ContactDetailsScreen(
                            state = state,
                            onNavigateBack = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onHandleContactDetailsAction = viewModel::handleContactDetailsAction
                        )
                    }
                    entry<Screen.Dialer> {
                        val viewModel = koinViewModel<CallLogsViewModel>()
                        val callViewModel = koinViewModel<CallingViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val callLogs = viewModel.callLogs.collectAsLazyPagingItems()

                        ObserveAsEvents(callViewModel.events) { event ->
                            when (event) {
                                is CallEvents.RequestDefaultDialer -> requestDefaultDialer()
                            }
                        }

                        val simPicker by callViewModel.simPicker.collectAsStateWithLifecycle()
                        simPicker?.let { picker ->
                            SimPickerDialog(
                                state = picker,
                                onPickSim = { callViewModel.handleCallAction(CallAction.PickSim(it)) },
                                onDismissRequest = { callViewModel.handleCallAction(CallAction.DismissSimPicker) }
                            )
                        }

                        CallLogsScreen(
                            state = state,
                            callLogs = callLogs,
                            textFieldState = viewModel.textFieldState,
                            onNavigate = backStack::navigate,
                            onHandleCallActions = callViewModel::handleCallAction,
                            onHandleDialerActions = viewModel::handleDialerAction
                        )
                    }

                    entry<Screen.Voicemail> {
                        val viewModel = koinViewModel<VoicemailViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        VoicemailScreen(
                            state = state,
                            textFieldState = viewModel.textFieldState,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onDeleteVoicemails = viewModel::deleteVoicemails
                        )
                    }

                    entry<Screen.Conversations> {

                        val viewModel = koinViewModel<ConversationsViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val conversations = viewModel.conversations.collectAsLazyPagingItems()

                        ConversationsScreen(
                            state = state,
                            conversations = conversations,
                            textFieldState = viewModel.textFieldState,
                            onNavigate = backStack::navigate,
                            onDefaultSmsAppChanged = viewModel::onDefaultSmsAppChanged,
                            onHandleConversationsAction = viewModel::handleThreadsAction
                        )
                    }

                    entry<Screen.ConversationDetails> { key ->
                        val viewModel = koinViewModel<ConversationDetailsViewModel>(
                            parameters = { parametersOf(key.threadId) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val messages = viewModel.messages.collectAsLazyPagingItems()

                        LaunchedEffect(key.searchQuery) {
                            if (key.searchQuery.isNotBlank()) {
                                viewModel.handleConversationActions(
                                    ConversationActions.OpenSearchWithQuery(key.searchQuery)
                                )
                            }
                        }
                        val lifecycleOwner = LocalLifecycleOwner.current
                        val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

                        ObserveAsEvents(viewModel.events) { event ->
                            when (event) {
                                is ConversationDetailsEvents.RequestDefaultSmsApp -> requestDefaultSmsApp()
                                is ConversationDetailsEvents.RequestDefaultDialer -> requestDefaultDialer()
                                else -> showConversationEventToast(context, resources, event)
                            }
                        }

                        // when new messages, block notifs for thread and remark it as read
                        val newestMessage = if (messages.itemCount > 0) messages.peek(0) else null
                        LaunchedEffect(newestMessage, lifecycleState) {
                            if (lifecycleState == Lifecycle.State.RESUMED) {
                                viewModel.handleConversationActions(ConversationActions.MarkAsRead)
                                viewModel.handleConversationActions(ConversationActions.ClearThreadNotifications)
                            }
                        }

                        RetainedEffect(state.settings.color) {
                            ChatColor.color = state.settings.color
                            onRetire { ChatColor.color = null }
                        }
                        val simPicker by viewModel.simPicker.collectAsStateWithLifecycle()
                        simPicker?.let { picker ->
                            SimPickerDialog(
                                state = picker,
                                onPickSim = { viewModel.handleConversationActions(ConversationActions.PickSim(it)) },
                                onDismissRequest = { viewModel.handleConversationActions(ConversationActions.DismissSimPicker) }
                            )
                        }

                        ConversationDetailsScreen(
                            state = state,
                            messages = messages,
                            searchTextFieldState = viewModel.searchTextFieldState,
                            prefilledMessage = key.prefilledMessage,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onDeleteConversation = viewModel::deleteConversation,
                            onHandleConversationSettingsActions = viewModel::handleConversationSettingsActions,
                            onHandleConversationActions = viewModel::handleConversationActions
                        )

                    }

                    entry<Screen.ConversationTheming> { key ->
                        val viewModel = koinViewModel<ThemingViewModel>(
                            parameters = { parametersOf(key.threadId) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ConversationTheming(
                            state = state,
                            onHandleConversationSettingsActions = viewModel::handleConversationSettingsActions,
                            onNavigateBack = backStack::navigateBack
                        )
                    }

                    entry<Screen.Dialpad> { key ->
                        val callViewModel = koinViewModel<CallingViewModel>()
                        val viewModel = koinViewModel<DialpadViewModel>(
                            parameters = { parametersOf(key.prefilledNumber) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val contacts = viewModel.contacts.collectAsLazyPagingItems()

                        ObserveAsEvents(callViewModel.events) { event ->
                            when (event) {
                                is CallEvents.RequestDefaultDialer -> requestDefaultDialer()
                            }
                        }

                        val simPicker by callViewModel.simPicker.collectAsStateWithLifecycle()
                        simPicker?.let { picker ->
                            SimPickerDialog(
                                state = picker,
                                onPickSim = { callViewModel.handleCallAction(CallAction.PickSim(it)) },
                                onDismissRequest = { callViewModel.handleCallAction(CallAction.DismissSimPicker) }
                            )
                        }

                        DialpadScreen(
                            state = state,
                            contacts = contacts,
                            textFieldState = viewModel.textFieldState,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onHandleCallAction = callViewModel::handleCallAction
                        )
                    }

                    entry<Screen.StartConversation> {

                        val viewModel = koinViewModel<StartConversationViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val contacts = viewModel.contacts.collectAsLazyPagingItems()

                        StartConversation(
                            state = state,
                            contacts = contacts,
                            textFieldState = viewModel.textFieldState,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onToggleGroupChatMode = viewModel::toggleGroupChatMode,
                            onAddNumberToGroup = viewModel::addNumberToGroup
                        )
                    }

                    entry<Screen.AboutConversation> { key ->
                        val viewModel = koinViewModel<ConversationDetailsViewModel>(
                            parameters = { parametersOf(key.threadId) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val media by viewModel.media.collectAsStateWithLifecycle()

                        ObserveAsEvents(viewModel.events) { event ->
                            when (event) {
                                is ConversationDetailsEvents.RequestDefaultSmsApp -> requestDefaultSmsApp()
                                is ConversationDetailsEvents.RequestDefaultDialer -> requestDefaultDialer()
                                else -> showConversationEventToast(context, resources, event)
                            }
                        }

                        val simPicker by viewModel.simPicker.collectAsStateWithLifecycle()
                        simPicker?.let { picker ->
                            SimPickerDialog(
                                state = picker,
                                onPickSim = { viewModel.handleConversationActions(ConversationActions.PickSim(it)) },
                                onDismissRequest = { viewModel.handleConversationActions(ConversationActions.DismissSimPicker) }
                            )
                        }

                        AboutConversationScreen(
                            state = state,
                            medias = media,
                            onNavigateBack = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onDeleteConversation = {
                                viewModel.deleteConversation()
                                backStack.navigate(Screen.Conversations)
                                backStack.removeAll {
                                    it is Screen.AboutConversation || it is Screen.ConversationDetails
                                }
                            },
                            onHandleConversationActions = viewModel::handleConversationActions
                        )
                    }

                    entry<Screen.Forwarding> { key ->
                        val viewModel = koinViewModel<ForwardingViewModel>(
                            parameters = { parametersOf(key.messageToForward) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ForwardingScreen(
                            state = state,
                            onNavigateBack = backStack::navigateBack,
                            onNavigate = { screen ->
                                backStack.navigate(screen)
                                backStack.removeAll { it is Screen.Forwarding }
                            }
                        )
                    }

                    entry<Screen.ArchivedThreads> {

                        val viewModel = koinViewModel<ArchivedConversationsViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ArchivedConversationsScreen(
                            state = state,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onHandleThreadsAction = viewModel::handleThreadsAction
                        )
                    }

                    entry<Screen.Settings> {
                        SettingsScreen(
                            onNavigateUp = backStack::navigateBack
                        )
                    }

                    entry<Screen.ContactEditor> { key ->

                        val viewModel = koinViewModel<EditContactViewModel>(
                            parameters = {
                                val id = key.rawContactId ?: Long.MAX_VALUE
                                parametersOf(id, key.prefilledNumber)
                            }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ObserveAsEvents(viewModel.events) { event ->
                            when (event) {
                                is EditContactEvent.Error -> {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.generic_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                is EditContactEvent.Success -> backStack.navigateBack()
                            }
                        }

                        EditContactScreen(
                            state = state,
                            onNavigateUp = backStack::navigateBack,
                            onHandleContactSettingsAction = viewModel::handleContactSettingsAction,
                            onHandeEditContactAction = viewModel::handleEditContactAction
                        )
                    }
                }
            )
        }
    }

}


private fun showConversationEventToast(
    context: Context,
    resources: Resources,
    event: ConversationDetailsEvents
) {
    val text = when (event) {
        is ConversationDetailsEvents.MmsSave -> {
            if (event.success) {
                resources.getString(R.string.saved)
            } else {
                resources.getString(R.string.failed_to_save_mms)
            }
        }

        is ConversationDetailsEvents.Block -> {
            val numbers = event.numbers
            if (numbers.size > 1) {
                resources.getString(
                    if (event.success) R.string.blocked_multiple
                    else R.string.block_multiple_fail,
                    numbers.first(),
                    numbers.size - 1
                )
            } else {
                resources.getString(
                    if (event.success) R.string.blocked_one
                    else R.string.block_one_fail,
                    numbers.first()
                )
            }
        }

        // handled by the nav entries, they launch a role request instead of a toast
        is ConversationDetailsEvents.RequestDefaultSmsApp,
        is ConversationDetailsEvents.RequestDefaultDialer -> return
    }

    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
