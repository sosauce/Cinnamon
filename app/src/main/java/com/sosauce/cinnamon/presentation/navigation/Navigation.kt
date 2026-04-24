package com.sosauce.cinnamon.presentation.navigation

import android.content.Intent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sosauce.cinnamon.data.datastore.PreferencesKeys.DEFAULT_TAB
import com.sosauce.cinnamon.data.datastore.dataStore
import com.sosauce.cinnamon.presentation.screens.archived.ArchivedThreads
import com.sosauce.cinnamon.presentation.screens.archived.ArchivedViewModel
import com.sosauce.cinnamon.presentation.screens.contacts.AboutMeScreen
import com.sosauce.cinnamon.presentation.screens.contacts.ContactDetailsScreen
import com.sosauce.cinnamon.presentation.screens.contacts.ContactDetailsViewModel
import com.sosauce.cinnamon.presentation.screens.contacts.ContactsScreen
import com.sosauce.cinnamon.presentation.screens.contacts.ContactsViewModel
import com.sosauce.cinnamon.presentation.screens.contacts.editor.EditContactScreen
import com.sosauce.cinnamon.presentation.screens.contacts.editor.EditContactViewModel
import com.sosauce.cinnamon.presentation.screens.dialer.DialerScreen
import com.sosauce.cinnamon.presentation.screens.dialer.DialerViewModel
import com.sosauce.cinnamon.presentation.screens.dialer.DialpadScreen
import com.sosauce.cinnamon.presentation.screens.dialer.DialpadViewModel
import com.sosauce.cinnamon.presentation.screens.messages.ConversationActions
import com.sosauce.cinnamon.presentation.screens.messages.ConversationDetailsViewModel
import com.sosauce.cinnamon.presentation.screens.messages.ConversationScreen
import com.sosauce.cinnamon.presentation.screens.messages.ConversationsScreen
import com.sosauce.cinnamon.presentation.screens.messages.ConversationsViewModel
import com.sosauce.cinnamon.presentation.screens.starter.StartConversation
import com.sosauce.cinnamon.presentation.screens.phone.CallingViewModel
import com.sosauce.cinnamon.presentation.screens.settings.SettingsScreen
import com.sosauce.cinnamon.presentation.screens.starter.StartConversationViewModel
import com.sosauce.cinnamon.presentation.screens.voicemail.VoicemailScreen
import com.sosauce.cinnamon.presentation.screens.voicemail.VoicemailViewModel
import com.sosauce.cinnamon.presentation.screens.wallpaper.ConversationTheming
import com.sosauce.cinnamon.presentation.screens.wallpaper.ThemingViewModel
import com.sosauce.cinnamon.utils.DefaultTabOption
import com.sosauce.cinnamon.utils.LocalHazeState
import com.sosauce.cinnamon.utils.LocalScreen
import com.sosauce.cinnamon.utils.bouncySpec
import com.sosauce.cinnamon.utils.navigateBack
import com.sosauce.cinnamon.utils.rememberHazeState
import com.sosauce.cinnamon.utils.tabToScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun Nav(
    intent: Intent?,
    onUpdateSeedColor: (Color) -> Unit
) {

    val context = LocalContext.current
    val initialTab = remember {
        // meh, but works
        runBlocking { context.dataStore.data.map { it[DEFAULT_TAB] }.first() ?: DefaultTabOption.MESSAGES }
    }
    val backStack = rememberNavBackStack(initialTab.tabToScreen()).apply {
        handleIntent(intent)
    }
    val hazeState = rememberHazeState()



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
                predictivePopTransitionSpec = { ContentTransform(fadeIn(), fadeOut()) },
                entryProvider = entryProvider {

                    entry<Screen.Contacts> {
                        val viewModel = koinViewModel<ContactsViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ContactsScreen(
                            state = state,
                            onNavigate = backStack::add,
                            onHandleContactsAction = viewModel::handleContactsAction
                        )
                    }

                    entry<Screen.ContactDetails> { key ->

                        val viewModel = koinViewModel<ContactDetailsViewModel>(
                            parameters = { parametersOf(key.contactId) }
                        )
                        val callViewModel = koinViewModel<CallingViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ContactDetailsScreen(
                            state = state,
                            onNavigateBack = backStack::navigateBack,
                            onNavigate = backStack::add,
                            onHandleCallAction = callViewModel::handleCallAction,
                            onHandleContactDetailsAction = viewModel::handleContactDetailsAction
                        )
                    }
                    entry<Screen.Dialer> {
                        val viewModel = koinViewModel<DialerViewModel>()
                        val callViewModel = koinViewModel<CallingViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        DialerScreen(
                            state = state,
                            onNavigate = backStack::add,
                            onHandleCallActions = callViewModel::handleCallAction,
                            onDeleteCallLogs = viewModel::deleteCallLogs
                        )
                    }

                    entry<Screen.Voicemail> {
                        val viewModel = koinViewModel<VoicemailViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        VoicemailScreen(
                            state = state,
                            onNavigateUp = backStack::navigateBack
                        )
                    }

                    entry<Screen.Messages> {

                        val viewModel = koinViewModel<ConversationsViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ConversationsScreen(
                            state = state,
                            onNavigate = backStack::add,
                            onHandleThreadsAction = viewModel::handleThreadsAction
                        )
                    }

                    entry<Screen.Conversation>(
                        metadata = NavDisplay.transitionSpec {
                            slideInHorizontally(bouncySpec()) { -it } + fadeIn() togetherWith  fadeOut()
                        }
                    ) { key ->
                        val viewModel = koinViewModel<ConversationDetailsViewModel>(
                            parameters = { parametersOf(key.threadId) }
                        )
                        val callViewModel = koinViewModel<CallingViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) {
                            viewModel.handleConversationActions(ConversationActions.MarkAsRead)
                            viewModel.handleConversationActions(ConversationActions.ClearThreadNotifications)
                        }


                        ConversationScreen(
                            state = state,
                            onNavigateUp = backStack::navigateBack,
                            onHandleCallAction = callViewModel::handleCallAction,
                            onNavigate = backStack::add,
                            onDeleteConversation = viewModel::deleteConversation,
                            onHandleConversationSettingsActions = viewModel::handleConversationSettingsActions,
                            onHandleConversationActions = viewModel::handleConversationActions,
                            onUpdateSeedColor = onUpdateSeedColor
                        )
                    }

                    entry<Screen.ConversationTheming> { key ->
                        val viewModel = koinViewModel<ThemingViewModel>(
                            parameters = { parametersOf(key.threadId) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ConversationTheming(
                            state = state,
                            threadId = key.threadId,
                            onHandleConversationSettingsActions = viewModel::handleConversationSettingsActions,
                            onNavigateBack = backStack::navigateBack
                        )
                    }

                    entry<Screen.Dialpad> {
                        val callViewModel = koinViewModel<CallingViewModel>()
                        val viewModel = koinViewModel<DialpadViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        DialpadScreen(
                            state = state,
                            onNavigate = backStack::add,
                            onNavigateUp = backStack::navigateBack,
                            onHandleCallAction = callViewModel::handleCallAction
                        )
                    }

                    entry<Screen.StartConversation>(
                        metadata = NavDisplay.transitionSpec {
                            slideInVertically(bouncySpec()) { it } + fadeIn() togetherWith fadeOut()
                        }
                    ) {

                        val viewModel = koinViewModel<StartConversationViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        StartConversation(
                            state = state,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::add
                        )
                    }

                    entry<Screen.AboutMe>{
                        AboutMeScreen(
                            onNavigateBack = backStack::navigateBack
                        )
                    }

                    entry<Screen.ArchivedThreads> {

                        val viewModel = koinViewModel<ArchivedViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        ArchivedThreads(
                            state = state,
                            onNavigateUp = backStack::navigateBack,
                            onNavigate = backStack::add,
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
                            parameters = { parametersOf(key.contact) }
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()


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
