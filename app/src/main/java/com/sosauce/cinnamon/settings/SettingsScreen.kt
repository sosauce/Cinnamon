package com.sosauce.cinnamon.settings

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.SettingsScreens
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.core.utils.navigateBack
import com.sosauce.cinnamon.settings.components.AboutCard
import com.sosauce.cinnamon.settings.components.SettingsCategoryCard
import com.sosauce.cinnamon.settings.components.SettingsNavigation

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit
) {
    val backStack = rememberNavBackStack(SettingsScreens.Settings)
    val scrollState = rememberScrollState()
    val items = listOf(
        Item(
            icon = R.drawable.phone_filled,
            name = stringResource(R.string.permissions),
            description = stringResource(R.string.permissions_desc),
            onNavigate = { backStack.add(SettingsScreens.Permissions) }
        ),
        Item(
            icon = R.drawable.palette,
            name = stringResource(R.string.look_and_feel),
            description = stringResource(R.string.look_and_feel_desc),
            onNavigate = { backStack.add(SettingsScreens.LookAndFeel) }
        ),
        Item(
            icon = R.drawable.navigation,
            name = stringResource(R.string.navigation),
            description = stringResource(R.string.navigation_desc),
            onNavigate = { backStack.add(SettingsScreens.Navigation) }
        ),
//        Item(
//            icon = R.drawable.behavior,
//            name = stringResource(R.string.behavior),
//            description = stringResource(R.string.behavior_desc),
//            onNavigate = { backStack.add(SettingsScreens.Behavior) }
//        ),
        Item(
            icon = R.drawable.message_rounded,
            name = stringResource(R.string.messages),
            description = stringResource(R.string.messages_settings_desc),
            onNavigate = { backStack.add(SettingsScreens.Messages) }
        ),
        Item(
            icon = R.drawable.contacts,
            name = stringResource(R.string.contacts),
            description = stringResource(R.string.contacts_settings_desc),
            onNavigate = { backStack.add(SettingsScreens.Contacts) }
        ),
        Item(
            icon = R.drawable.phone,
            name = stringResource(R.string.phone),
            description = stringResource(R.string.phone_settings_desc),
            onNavigate = { backStack.add(SettingsScreens.Phone) }
        ),
//        Item(
//            icon = R.drawable.migrate,
//            name = stringResource(R.string.migration),
//            description = stringResource(R.string.migration_desc),
//            onNavigate = { backStack.add(SettingsScreens.Migration) }
//        )
    )


    Scaffold(
        bottomBar = {
            CuteNavigationButton(
                onNavigateUp = {
                    if (backStack.size == 1) {
                        onNavigateUp()
                    } else {
                        backStack.navigateBack()
                    }
                }
            )
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(paddingValues),
            onBack = {
                if (backStack.size == 1) {
                    onNavigateUp()
                } else {
                    backStack.navigateBack()
                }
            },
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

                entry<SettingsScreens.Settings> {
                    Column {
                        AboutCard()
                        Spacer(Modifier.height(20.dp))
                        items.fastForEachIndexed { index, item ->
                            SettingsCategoryCard(
                                icon = item.icon,
                                name = item.name,
                                description = item.description,
                                topDp = if (index == 0) 24.dp else 4.dp,
                                bottomDp = if (index == items.lastIndex) 24.dp else 4.dp,
                                onNavigate = item.onNavigate
                            )
                        }
                    }
                }

                entry<SettingsScreens.Messages> {
                    SettingsMessages()
                }
                entry<SettingsScreens.Navigation> {
                    SettingsNavigation()
                }
                entry<SettingsScreens.Contacts> {
                    SettingsContacts()
                }
                entry<SettingsScreens.Phone> {
                    SettingsPhone()
                }
                entry<SettingsScreens.LookAndFeel> {
                    SettingsLookAndFeel()
                }
                entry<SettingsScreens.Behavior> {
                    SettingsBehavior()
                }
                entry<SettingsScreens.Permissions> {
                    SettingsPermissions()
                }
            }
        )
    }
}

private data class Item(
    val name: String,
    val description: String,
    val icon: Int,
    val onNavigate: () -> Unit
)