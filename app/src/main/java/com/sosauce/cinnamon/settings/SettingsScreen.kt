@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
import com.sosauce.cinnamon.core.ui.components.AppIcon
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.core.ui.groupedShape
import com.sosauce.cinnamon.core.utils.GITHUB_RELEASES
import com.sosauce.cinnamon.core.utils.SUPPORT_PAGE
import com.sosauce.cinnamon.core.utils.appVersion
import com.sosauce.cinnamon.core.utils.rememberInteractionSource
import com.sosauce.cinnamon.settings.components.SettingsNavigation
import com.sosauce.cinnamon.settings.components.SettingsPage
import com.sosauce.nekobites.animations.rememberAnimatedShape

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
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer
    )
    val shapes = listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Sunny,
        MaterialShapes.Pentagon,
        MaterialShapes.Gem
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
        AboutCard()
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
                val (container, content) = palettes[index % palettes.size]
                SettingsRow(
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    badgeShape = shapes[index % shapes.size].toShape(),
                    badgeContainer = container,
                    badgeContent = content
                )
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)
    val badgeShape = rememberAnimatedShape(
        condition = isPressed,
        shapeA = MaterialShapes.Flower,
        shapeB = MaterialShapes.Circle
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
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
                    .background(colors.primary, badgeShape)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colors.onPrimary,
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.primary, CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    val buttonHeight = ButtonDefaults.MediumContainerHeight
    val interactionSources = List(2) { rememberInteractionSource() }
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing))
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainer,
        contentColor = colors.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(unbounded = true)
                        .offset(x = 40.dp, y = (-40).dp)
                        .size(150.dp)
                        .rotate(rotation)
                        .background(
                            colors.primaryContainer.copy(alpha = 0.6f),
                            MaterialShapes.Cookie12Sided.toShape()
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .wrapContentSize(unbounded = true)
                        .offset(x = (-50).dp, y = 30.dp)
                        .size(90.dp)
                        .rotate(-rotation)
                        .background(
                            colors.tertiaryContainer.copy(alpha = 0.6f),
                            MaterialShapes.Cookie9Sided.toShape()
                        )
                )
            }
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(100.dp)
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMediumEmphasized
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = colors.onSurface.copy(alpha = 0.1f),
                            contentColor = colors.onSurface
                        ) {
                            Text(
                                text = "v${context.appVersion}",
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                ) {
                    Button(
                        onClick = { uriHandler.openUri(GITHUB_RELEASES) },
                        shapes = ButtonShapes(
                            shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                            pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceContainerLowest,
                            contentColor = colors.onSurface
                        ),
                        interactionSource = interactionSources[0],
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                    ) {
                        AboutButtonContent(R.drawable.github, stringResource(R.string.github))
                    }
                    Button(
                        onClick = { uriHandler.openUri(SUPPORT_PAGE) },
                        shapes = ButtonShapes(
                            shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                            pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape
                        ),
                        interactionSource = interactionSources[1],
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                    ) {
                        AboutButtonContent(R.drawable.favorite_filled, stringResource(R.string.support))
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutButtonContent(
    @DrawableRes icon: Int,
    text: String
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.MediumIconSize)
    )
    Spacer(Modifier.width(8.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        maxLines = 1
    )
}

@Composable
private fun SettingsRow(
    entry: SettingsEntry,
    isFirst: Boolean,
    isLast: Boolean,
    badgeShape: Shape,
    badgeContainer: Color,
    badgeContent: Color
) {
    Surface(
        onClick = entry.onClick,
        shape = groupedShape(isFirst = isFirst, isLast = isLast),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .background(badgeContainer, badgeShape)
            ) {
                Icon(
                    painter = painterResource(entry.icon),
                    contentDescription = null,
                    tint = badgeContent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Immutable
private data class SettingsEntry(
    @param:DrawableRes val icon: Int,
    val name: String,
    val description: String,
    val onClick: () -> Unit
)
