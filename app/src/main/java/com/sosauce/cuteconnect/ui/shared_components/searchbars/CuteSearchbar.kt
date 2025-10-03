@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cuteconnect.ui.shared_components.searchbars

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.ui.shared_components.ScreenSelection
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.theme.nunitoFontFamily
import com.sosauce.cuteconnect.utils.LocalScreen
import com.sosauce.cuteconnect.utils.rememberSearchbarMaxFloatValue
import com.sosauce.cuteconnect.utils.rememberSearchbarRightPadding
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlin.collections.get

@Composable
fun CuteSearchbar(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    sortingMenu: @Composable (() -> Unit),
    navigationIcon: @Composable (() -> Unit)? = null,
    showSearchField: Boolean = true,
    fab: @Composable () -> Unit,
    onNavigate: (Screen) -> Unit,
) {

    val screenToLeadingIcon =
        mapOf(
            Screen.Messages to R.drawable.message_rounded,
            Screen.Contacts to R.drawable.contacts,
            Screen.Dialer to R.drawable.phone,
        )
    val currentScreen = LocalScreen.current
    var isInScreenSelectionMode by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    //val showBackButton by rememberShowBackButton()
    val showBackButton = true



    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(rememberSearchbarMaxFloatValue())
            .padding(end = rememberSearchbarRightPadding())
            .imePadding()

    ) {

        Row(
            horizontalArrangement = if (navigationIcon != null) Arrangement.SpaceBetween else Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showBackButton) {
                navigationIcon?.invoke()
            }
            fab()
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AnimatedVisibility(
                visible = showSearchField
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(6.dp)
                ) {
                    AnimatedContent(
                        targetState = isInScreenSelectionMode,
                        transitionSpec = { scaleIn() togetherWith scaleOut() }
                    ) {
                        if (it) {
                            ScreenSelection(
                                screenToLeadingIcon = screenToLeadingIcon,
                                onNavigate = onNavigate,
                                dismiss = { isInScreenSelectionMode = false }
                            )
                        } else {
                            TextField(
                                state = textFieldState,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                placeholder = {
                                    CuteText(
                                        text = stringResource(R.string.search_here),
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {
                                    IconButton(
                                        onClick = { isInScreenSelectionMode = true }
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                screenToLeadingIcon[currentScreen]
                                                    ?: R.drawable.search
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row {

                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false },
                                            shape = RoundedCornerShape(24.dp)
                                        ) {
                                            sortingMenu()
                                        }
                                        IconButton(
                                            onClick = { showSortMenu = !showSortMenu }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                                contentDescription = stringResource(R.string.sort)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onNavigate(Screen.Settings) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Settings,
                                                contentDescription = stringResource(R.string.settings)
                                            )
                                        }
                                    }
                                },
                                textStyle = TextStyle.Default.copy(
                                    fontFamily = nunitoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                lineLimits = TextFieldLineLimits.SingleLine,
                                shape = FloatingToolbarDefaults.ContainerShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                    }
                }
            }
        }
    }
}






