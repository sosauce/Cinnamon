@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.logs

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.datastore.rememberSortLogsAscending
import com.sosauce.cinnamon.core.ui.components.NoResult
import com.sosauce.cinnamon.core.ui.components.SelectedBarAction
import com.sosauce.cinnamon.core.ui.components.SelectedBarActions
import com.sosauce.cinnamon.core.ui.components.SelectedBarSurface
import com.sosauce.cinnamon.core.ui.components.menus.SortingDropdownMenu
import com.sosauce.cinnamon.core.ui.components.searchbars.MainSearchbar
import com.sosauce.cinnamon.core.utils.LazyListKeys
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.phone.domain.CallPresentation
import com.sosauce.cinnamon.features.phone.domain.CuteCallLog
import com.sosauce.cinnamon.setup.SetupDefaultDialerScreen
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.sweetselect.rememberSweetSelectState

@Composable
fun SharedTransitionScope.CallLogsScreen(
    state: CallLogsState,
    callLogs: LazyPagingItems<CallLogsListItem>,
    textFieldState: TextFieldState,
    onNavigate: (Screen) -> Unit,
    onHandleCallLogsActions: (CallLogsAction) -> Unit
) {
    val sweetSelectState = rememberSweetSelectState<CuteCallLog>()
    var sortLogsAscending by rememberSortLogsAscending()
    val isDefaultDialer by PermissionUtils.observeHasDefaultApp(PermissionUtils.DIALER)

    LaunchedEffect(isDefaultDialer) {
        onHandleCallLogsActions(CallLogsAction.DefaultDialerChanged(isDefaultDialer))
    }

    Scaffold(
        bottomBar = {
            MainSearchbar(
                textFieldState = textFieldState,
                isInSelectionMode = sweetSelectState.isInSelectionMode,
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
                                    onHandleCallLogsActions(
                                        CallLogsAction.ChangeFilter(
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
                // dialing has its own tab, see Screen.Dialer
                fab = {},
                selectedBar = {
                    // loaded logs only
                    val items =
                        callLogs.itemSnapshotList.items.mapNotNull { (it as? CallLogsListItem.Log)?.log }

                    SelectedBarSurface(
                        modifier = Modifier.selfAlignHorizontally(),
                        items = items,
                        multiSelectState = sweetSelectState
                    ) {
                        SelectedBarActions(
                            actions = listOf(
                                SelectedBarAction(
                                    icon = R.drawable.delete_filled,
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        val logs = sweetSelectState.selectedItems.map { it.id }
                                        onHandleCallLogsActions(CallLogsAction.DeleteLogs(logs))
                                        sweetSelectState.clearSelected()
                                    }
                                )
                            )
                        )
                    }
                },
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->

        if (!isDefaultDialer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SetupDefaultDialerScreen(
                    onNext = { }
                )
            }
            return@Scaffold
        }

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

                if (callLogs.itemCount == 0 && callLogs.loadState.refresh is LoadState.NotLoading && !state.isLoading) {
                    item(LazyListKeys.EMPTY_STATE) {
                        if (state.isSearching) {
                            NoResult()
                        } else {
                            NoXFound(
                                headlineText = R.string.no_calls_found,
                                bodyText = R.string.no_calls_found_desc,
                                icon = R.drawable.call_log
                            )
                        }
                    }
                } else {
                    items(
                        count = callLogs.itemCount,
                        key = callLogs.itemKey { it.key },
                        contentType = callLogs.itemContentType { it::class }
                    ) { index ->
                        when (val item = callLogs[index]) {
                            is CallLogsListItem.Header -> {
                                Text(
                                    text = item.date,
                                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }

                            is CallLogsListItem.Log -> {
                                val callLog = item.log
                                val isSelected by sweetSelectState.isSelectedAsState(callLog)

                                CallLogItem(
                                    modifier = Modifier.animateItem(),
                                    callLog = callLog,
                                    isSelected = isSelected,
                                    numberOfAppearance = item.count,
                                    onCallLogsAction = onHandleCallLogsActions,
                                    onNavigate = onNavigate,
                                    onDeleteCallLog = {
                                        onHandleCallLogsActions(
                                            CallLogsAction.DeleteLogs(
                                                listOf(callLog.id)
                                            )
                                        )
                                    },
                                    onClick = {
                                        if (sweetSelectState.isInSelectionMode) {
                                            sweetSelectState.toggle(callLog)
                                        } else {
                                            if (callLog.presentation == CallPresentation.ALLOWED) {
                                                onHandleCallLogsActions(CallLogsAction.CallNumber(callLog.number))
                                            }
                                        }
                                    },
                                    onLongClick = { sweetSelectState.toggle(callLog) }
                                )
                            }

                            null -> Unit // placeholders are disabled
                        }
                    }
                }
            }
        }
    }
}
