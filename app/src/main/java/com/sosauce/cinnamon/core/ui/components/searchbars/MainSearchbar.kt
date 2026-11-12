package com.sosauce.cinnamon.core.ui.components.searchbars

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.barsContentTransform
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally

val MainTabs: List<Screen> = listOf(Screen.Conversations, Screen.Contacts, Screen.Dialer)

@Composable
fun SharedTransitionScope.MainSearchbar(
    textFieldState: TextFieldState,
    isInSelectionMode: Boolean,
    sortingMenu: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    selectedBar: @Composable () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val navAnimatedScope = LocalNavAnimatedContentScope.current

    AnimatedContent(
        targetState = isInSelectionMode,
        transitionSpec = { barsContentTransform }
    ) { inSelectionMode ->
        if (inSelectionMode) {
            selectedBar()
        } else {
            CuteSearchbar2(
                modifier = Modifier.selfAlignHorizontally(),
                barModifier = Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(SharedTransitionKeys.MAIN_SEARCHBAR),
                    animatedVisibilityScope = navAnimatedScope
                ),
                fabRowModifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(SharedTransitionKeys.MAIN_SEARCHBAR_FAB),
                    animatedVisibilityScope = navAnimatedScope
                ),
                textFieldState = textFieldState,
                sortingMenu = sortingMenu,
                fab = fab,
                onNavigate = onNavigate
            )
        }
    }
}
