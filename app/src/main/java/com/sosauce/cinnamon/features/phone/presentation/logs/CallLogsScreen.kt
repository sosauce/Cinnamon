@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.logs

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.datastore.rememberSortLogsAscending
import com.sosauce.cinnamon.core.ui.components.NoResult
import com.sosauce.cinnamon.core.ui.components.SelectedBarSurface
import com.sosauce.cinnamon.core.ui.components.menus.SortingDropdownMenu
import com.sosauce.cinnamon.core.ui.components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.core.utils.LazyListKeys
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.phone.domain.CallPresentation
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog2
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.sweetselect.rememberSweetSelectState

@Composable
fun CallLogsScreen(
    state: CallLogsState,
    textFieldState: TextFieldState,
    onNavigate: (Screen) -> Unit,
    onHandleCallActions: (CallAction) -> Unit,
    onHandleDialerActions: (DialerAction) -> Unit
) {
    val sweetSelectState = rememberSweetSelectState<CuteCallLog2>()
    var sortLogsAscending by rememberSortLogsAscending()

    Scaffold(
        bottomBar = {

            AnimatedContent(
                targetState = sweetSelectState.isInSelectionMode
            ) {
                if (it) {
                    val items = state.callLogs.values.flatten().fastMap { it.first }

                    SelectedBarSurface(
                        modifier = Modifier.selfAlignHorizontally(),
                        items = items,
                        multiSelectState = sweetSelectState
                    ) {
                        ButtonGroup(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            overflowIndicator = {}
                        ) {
                            customItem(
                                buttonGroupContent = {
                                    Button(
                                        onClick = {
                                            val logs = sweetSelectState.selectedItems.map { it.id }
                                            onHandleDialerActions(DialerAction.DeleteLogs(logs))
                                            sweetSelectState.clearSelected()
                                        },
                                        shape = RoundedCornerShape(
                                            topStart = 50.dp,
                                            bottomStart = 50.dp,
                                            topEnd = 50.dp,
                                            bottomEnd = 50.dp
                                        ),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.delete_filled),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                menuContent = {}
                            )
                        }
                    }
                } else {
                    CuteSearchbar(
                        modifier = Modifier.selfAlignHorizontally(),
                        textFieldState = textFieldState,
                        sortingMenu = {
                            SortingDropdownMenu(
                                isSortedAscending = sortLogsAscending,
                                onChangeSorting = { sortLogsAscending = it }
                            ) {
                                repeat(5) { index ->

                                    val filter = when (index) {
                                        0 -> CallLogsFilter.ALL
                                        1 -> CallLogsFilter.CONTACTS
                                        2 -> CallLogsFilter.INCOMING
                                        3 -> CallLogsFilter.OUTGOING
                                        4 -> CallLogsFilter.MISSED
                                        else -> throw IndexOutOfBoundsException()
                                    }
                                    val text = when (index) {
                                        0 -> R.string.all
                                        1 -> R.string.contacts
                                        2 -> R.string.incoming
                                        3 -> R.string.outgoing
                                        4 -> R.string.missed
                                        else -> throw IndexOutOfBoundsException()
                                    }

                                    SelectableDropdownMenuItem(
                                        selected = filter == state.filter,
                                        onClick = {
                                            onHandleDialerActions(
                                                DialerAction.ChangeFilter(
                                                    filter
                                                )
                                            )
                                        },
                                        shapes = MenuDefaults.itemShapes(),
                                        text = {
                                            Text(
                                                text = stringResource(text)
                                            )
                                        }
                                    )
                                }
                            }
                        },
                        fab = {
                            AnimatedFab(
                                onClick = { onNavigate(Screen.Dialpad()) },
                                icon = R.drawable.dialpad
                            )
                        },
                        onNavigate = onNavigate
                    )

                }
            }
        }
    ) { paddingValues ->

        LoadingBox(
            isLoading = state.isLoading
        ) {
            LazyColumn(
                contentPadding = paddingValues
            ) {
                item(LazyListKeys.VOICEMAIL) {

                    FilledTonalButton(
                        onClick = { onNavigate(Screen.Voicemail) },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .animateItem()
                            .selfAlignHorizontally()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.voicemail),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.voicemail))
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (state.callLogs.isEmpty() && !state.isSearching) {
                    item {
                        NoXFound(
                            headlineText = R.string.no_calls_found,
                            bodyText = R.string.no_calls_found_desc,
                            icon = R.drawable.call_log_rounded
                        )
                    }
                } else {
                    if (state.callLogs.isNotEmpty()) {
                        state.callLogs.forEach { (date, logs) ->
                            item(
                                key = date
                            ) {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                            items(
                                items = logs,
                                key = { (calls, _) -> calls.id }
                            ) { (callLog, count) ->

                                val isSelected by sweetSelectState.isSelectedAsState(callLog)

                                CallLogItem(
                                    modifier = Modifier.animateItem(),
                                    callLog = callLog,
                                    isSelected = isSelected,
                                    numberOfAppearance = count,
                                    onCallAction = onHandleCallActions,
                                    onNavigate = onNavigate,
                                    onDeleteCallLog = {
                                        onHandleDialerActions(
                                            DialerAction.DeleteLogs(
                                                listOf(callLog.id)
                                            )
                                        )
                                    },
                                    onClick = {
                                        if (sweetSelectState.isInSelectionMode) {
                                            sweetSelectState.toggle(callLog)
                                        } else {
                                            if (callLog.presentation == CallPresentation.ALLOWED) {
                                                onHandleCallActions(CallAction.LaunchCall(callLog.number))
                                            }
                                        }
                                    },
                                    onLongClick = { sweetSelectState.toggle(callLog) }

                                )
                            }
                        }
                    } else {
                        item { NoResult() }
                    }
                }
            }
        }
    }
}