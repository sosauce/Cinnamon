@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.app.navigation

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberInitialScreenBlocking
import com.sosauce.cinnamon.core.ui.ChatColor
import com.sosauce.cinnamon.core.utils.LocalHazeState
import com.sosauce.cinnamon.core.utils.LocalScreen
import com.sosauce.cinnamon.core.utils.rememberHazeState
import com.sosauce.cinnamon.core.utils.tabToScreen
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsEvent
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
import com.sosauce.cinnamon.features.phone.presentation.call.CallingViewModel
import com.sosauce.cinnamon.features.phone.presentation.dialpad.DialpadScreen
import com.sosauce.cinnamon.features.phone.presentation.dialpad.DialpadViewModel
import com.sosauce.cinnamon.features.phone.presentation.logs.CallLogsScreen
import com.sosauce.cinnamon.features.phone.presentation.logs.CallLogsViewModel
import com.sosauce.cinnamon.features.phone.presentation.voicemail.VoicemailScreen
import com.sosauce.cinnamon.features.phone.presentation.voicemail.VoicemailViewModel
import com.sosauce.cinnamon.settings.SettingsScreen
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
    val hazeState = rememberHazeState()

    LaunchedEffect(intent) { backStack.handleIntent(context, intent) }

    CompositionLocalProvider(
        LocalScreen provides backStack.last(),
        LocalHazeState provides hazeState
    ) {
        SharedTransitionLayout {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally { it } + fadeIn(),
                        initialContentExit = slideOutHorizontally { -it / 4 } + fadeOut()
                    )
                },
                predictivePopTransitionSpec = {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally { -it / 4 } + fadeIn(),
                        initialContentExit = slideOutHorizontally { it } + fadeOut()
                    )
                },
                popTransitionSpec = {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally { -it / 4 } + fadeIn(),
                        initialContentExit = slideOutHorizontally { it } + fadeOut()
                    )
                },
                entryProvider = entryProvider {

                    entry<Screen.Contacts> {
                        val viewModel = koinViewModel<ContactsViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ContactsScreen(
                            state = state,
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
                            when(event) {
                                is ContactDetailsEvent.Error -> {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.delete_contact_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                is ContactDetailsEvent.Success -> backStack.navigateBack()
                            }
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

                        CallLogsScreen(
                            state = state,
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

                        ConversationsScreen(
                            state = state,
                            textFieldState = viewModel.textFieldState,
                            onNavigate = backStack::navigate,
                            onHandleConversationsAction = viewModel::handleThreadsAction
                        )
                    }

                    entry<Screen.ConversationDetails> { key ->
                        val viewModel = koinViewModel<ConversationDetailsViewModel>(
                            parameters = { parametersOf(key.threadId) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ObserveAsEvents(viewModel.events) { event ->
                            when(event) {
                                is ConversationDetailsEvents.MmsSave -> {
                                    val text = if (event.success) {
                                        resources.getString(R.string.saved)
                                    } else {
                                        resources.getString(R.string.failed_to_save_mms)
                                    }

                                    Toast.makeText(
                                        context,
                                        text,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                is ConversationDetailsEvents.Block -> {

                                    val numbers = event.numbers

                                    val text = if (numbers.size > 1) {
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

                                    Toast.makeText(
                                        context,
                                        text,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        // when new messages, block notifs for thread and remark it as read
                        LaunchedEffect(state.messages) {
                            viewModel.handleConversationActions(ConversationActions.MarkAsRead)
                            viewModel.handleConversationActions(ConversationActions.ClearThreadNotifications)
                        }

                        RetainedEffect(state.settings.color) {
                            ChatColor.color = state.settings.color
                            onRetire { ChatColor.color = null }
                        }
                        ConversationDetailsScreen(
                            state = state,
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

                        DialpadScreen(
                            state = state,
                            textFieldState = viewModel.textFieldState,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::navigate,
                            onHandleCallAction = callViewModel::handleCallAction
                        )
                    }

                    entry<Screen.StartConversation> {

                        val viewModel = koinViewModel<StartConversationViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        StartConversation(
                            state = state,
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
                        AboutConversationScreen(
                            state = state,
                            onNavigateBack = backStack::navigateBack,
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
                                    // error toast brrr
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

