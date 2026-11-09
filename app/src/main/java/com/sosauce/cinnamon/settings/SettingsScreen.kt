@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.SettingsScreens
import com.sosauce.cinnamon.app.navigation.navigateBack
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButton
import com.sosauce.nekobites.utils.groupedShape
import com.sosauce.cinnamon.core.utils.GITHUB_RELEASES
import com.sosauce.cinnamon.core.utils.rememberInteractionSource
import com.sosauce.cinnamon.settings.components.SettingsNavigation
import com.sosauce.nekobites.animations.rememberAnimatedShape
import com.sosauce.nekobites.components.settings.AboutCard
import com.sosauce.nekobites.components.settings.SettingsCard
import com.sosauce.nekobites.components.settings.SettingsEntry
import com.sosauce.nekobites.components.settings.SettingsPage

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit
) {
    val backStack = rememberNavBackStack(SettingsScreens.Settings)
    val goBack: () -> Unit = {
        if (backStack.size == 1) onNavigateUp() else backStack.navigateBack()
    }

    Scaffold(
        bottomBar = { CuteNavigationButton(onNavigateUp = goBack) }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
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
                    SettingsHome(
                        backStack = backStack,
                        contentPadding = paddingValues
                    )
                }
                entry<SettingsScreens.Messages> {
                    ScrollPage(paddingValues) { SettingsMessages() }
                }
                entry<SettingsScreens.Navigation> {
                    ScrollPage(paddingValues) { SettingsNavigation() }
                }
                entry<SettingsScreens.Contacts> {
                    ScrollPage(paddingValues) { SettingsContacts() }
                }
                entry<SettingsScreens.Phone> {
                    ScrollPage(paddingValues) { SettingsPhone() }
                }
                entry<SettingsScreens.LookAndFeel> {
                    ScrollPage(paddingValues) { SettingsLookAndFeel() }
                }
                entry<SettingsScreens.Behavior> {
                    ScrollPage(paddingValues) { SettingsBehavior() }
                }
                entry<SettingsScreens.Permissions> {
                    ScrollPage(paddingValues) { SettingsPermissions() }
                }
                entry<SettingsScreens.Backup> {
                    ScrollPage(paddingValues) { SettingsBackup() }
                }
            }
        )
    }
}

@Composable
private fun ScrollPage(
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsPage(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        content = content
    )
}

@Composable
private fun SettingsHome(
    backStack: NavBackStack<NavKey>,
    contentPadding: PaddingValues
) {
    val colors = MaterialTheme.colorScheme
    val palettes = listOf(
        colors.primary,
        colors.secondary,
        colors.tertiary
    )
    val entries = listOf(
        SettingsEntry(
            icon = R.drawable.palette,
            name = stringResource(R.string.look_and_feel),
            description = stringResource(R.string.look_and_feel_desc),
            onClick = { backStack.add(SettingsScreens.LookAndFeel) }
        ),
        SettingsEntry(
            icon = R.drawable.navigation,
            name = stringResource(R.string.navigation),
            description = stringResource(R.string.navigation_desc),
            onClick = { backStack.add(SettingsScreens.Navigation) }
        ),
        SettingsEntry(
            icon = R.drawable.message_rounded,
            name = stringResource(R.string.messages),
            description = stringResource(R.string.messages_settings_desc),
            onClick = { backStack.add(SettingsScreens.Messages) }
        ),
        SettingsEntry(
            icon = R.drawable.phone,
            name = stringResource(R.string.phone),
            description = stringResource(R.string.phone_settings_desc),
            onClick = { backStack.add(SettingsScreens.Phone) }
        ),
        SettingsEntry(
            icon = R.drawable.backup,
            name = stringResource(R.string.backup_and_restore),
            description = stringResource(R.string.backup_desc),
            onClick = { backStack.add(SettingsScreens.Backup) }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
            .padding(horizontal = 16.dp)
    ) {
        AboutCard(
            icon = R.drawable.message_rounded,
            appName = "Cinnamon",
            updateLink = GITHUB_RELEASES
        )
        Spacer(Modifier.height(20.dp))
        FeaturedCard(
            icon = R.drawable.security,
            title = stringResource(R.string.default_apps),
            description = stringResource(R.string.permissions_desc),
            onClick = { backStack.add(SettingsScreens.Permissions) }
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.fastForEachIndexed { index, entry ->
                val containerColor = palettes[index % palettes.size]
                SettingsCard(
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    badgeContainer = containerColor
                )
            }
        }
    }
}

//TODO: Import from Nekobites next release
@Composable
fun FeaturedCard(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = com.sosauce.nekobites.helpers.rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f
    )
    val shape = rememberAnimatedShape(
        condition = isPressed,
        shapeA = MaterialShapes.Cookie9Sided,
        shapeB = MaterialShapes.Circle
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        contentColor = contentColorFor(colors.surfaceContainerHigh),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.secondaryContainer, shape)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLargeEmphasized
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.secondaryContainer, CircleShape)
            ) {
                Icon(
                    painter = painterResource(com.sosauce.nekobites.R.drawable.arrow_right),
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
