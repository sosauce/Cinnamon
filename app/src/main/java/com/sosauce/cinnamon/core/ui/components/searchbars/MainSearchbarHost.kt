package com.sosauce.cinnamon.core.ui.components.searchbars

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.barsContentTransform
import com.sosauce.cinnamon.core.utils.LocalScreen
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally

val MainTabs: List<Screen> = listOf(Screen.Conversations, Screen.Contacts, Screen.Dialer)

@Stable
class MainSearchbarState {
    internal val contents = mutableStateMapOf<Screen, MainSearchbarContent>()
}

@Stable
class MainSearchbarContent(
    val textFieldState: TextFieldState,
    val sortingMenu: @Composable () -> Unit,
    val fab: @Composable () -> Unit,
    val selectedBar: @Composable () -> Unit
) {
    var isInSelectionMode by mutableStateOf(false)
        internal set
}

val LocalMainSearchbarState = staticCompositionLocalOf<MainSearchbarState> {
    throw IllegalStateException("No MainSearchbarState provided!")
}

@Composable
fun MainSearchbarEntry(
    screen: Screen,
    textFieldState: TextFieldState,
    isInSelectionMode: Boolean,
    sortingMenu: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    selectedBar: @Composable () -> Unit
) {
    val state = LocalMainSearchbarState.current
    val currentSortingMenu by rememberUpdatedState(sortingMenu)
    val currentFab by rememberUpdatedState(fab)
    val currentSelectedBar by rememberUpdatedState(selectedBar)

    val content = remember(screen, textFieldState) {
        MainSearchbarContent(
            textFieldState = textFieldState,
            sortingMenu = { currentSortingMenu() },
            fab = { currentFab() },
            selectedBar = { currentSelectedBar() }
        )
    }
    content.isInSelectionMode = isInSelectionMode

    DisposableEffect(state, screen, content) {
        state.contents[screen] = content
        onDispose { content.isInSelectionMode = false }
    }
}

@Composable
fun MainSearchbarHost(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentScreen = LocalScreen.current
    val state = LocalMainSearchbarState.current

    AnimatedVisibility(
        visible = currentScreen in MainTabs,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
    ) {
        val content = state.contents[currentScreen] ?: return@AnimatedVisibility

        AnimatedContent(
            targetState = content.isInSelectionMode,
            transitionSpec = { barsContentTransform }
        ) { isInSelectionMode ->
            CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
                if (isInSelectionMode) {
                    content.selectedBar()
                } else {
                    CuteSearchbar2(
                        modifier = Modifier.selfAlignHorizontally(),
                        textFieldState = content.textFieldState,
                        sortingMenu = content.sortingMenu,
                        fab = {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = { fadeIn() togetherWith fadeOut() }
                            ) { tab ->
                                state.contents[tab]?.fab?.invoke()
                            }
                        },
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}
