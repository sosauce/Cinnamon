@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.dialer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.google.common.collect.Multimaps.index
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberGroupSubsequentCalls
import com.sosauce.cinnamon.domain.model.CuteCallLog
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.shared_components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.utils.CuteRoundedCornerShape
import com.sosauce.cinnamon.utils.groupSubsequentlyBy
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import com.sosauce.cinnamon.utils.toDate
import com.sosauce.sweetselect.rememberSweetSelectState

@Composable
fun DialerScreen(
    state: DialerState,
    onNavigate: (Screen) -> Unit,
    onHandleCallActions: (CallAction) -> Unit,
    onDeleteCallLogs: (List<Long>) -> Unit
) {
    val sweetSelectState = rememberSweetSelectState<CuteCallLog>()
    val groupSubCalls by rememberGroupSubsequentCalls()
    val callLogs = state.callLogs.collectAsLazyPagingItems()

    Scaffold(
        bottomBar = {
            if (sweetSelectState.isInSelectionMode) {

            } else {
                CuteSearchbar(
                    modifier = Modifier.selfAlignHorizontally(),
                    sortingMenu = {},
                    fab = {
                        FloatingActionButton(
                            onClick = { onNavigate(Screen.Dialpad) },
                            shape = MaterialShapes.Cookie9Sided.toShape()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dialpad),
                                contentDescription = null
                            )
                        }
                    },
                    onNavigate = onNavigate
                )

            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(),
                    onClick = { onNavigate(Screen.Voicemail) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.voicemail),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.voicemail))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (groupSubCalls) {
//                state.callLogs
//                    .groupBy { it.date.toDate() }
//                    .forEach { (date, callLogs) ->
//
//                        val groupedCalls = callLogs.groupSubsequentlyBy { it.rawNumber }
//                        item {
//                            Text(
//                                text = date,
//                                style = MaterialTheme.typography.bodyLargeEmphasized.copy(
//                                    color = MaterialTheme.colorScheme.primary
//                                ),
//                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
//                            )
//                        }
//
//                        itemsIndexed(
//                            items = groupedCalls,
//                            key = { _, (call, _) -> call.id }
//                        ) { index, (callLog, appearances) ->
//                            CallLogItem(
//                                callLog = callLog,
//                                numberOfAppearance = appearances,
//                                onCallAction = onHandleCallActions,
//                                onNavigate = onNavigate,
//                                shape = CuteRoundedCornerShape(
//                                    top = if (index == 0) 24.dp else 4.dp,
//                                    bottom = if (index == groupedCalls.lastIndex) 24.dp else 0.dp
//                                ),
//                                onDeleteCallLog = { onDeleteCallLogs(listOf(callLog.id)) },
//                                modifier = Modifier.animateItem(),
//                                sweetSelectState = sweetSelectState
//                            )
//                        }
//                    }
            } else {
                items(
                    count = callLogs.itemCount,
                    key = callLogs.itemKey { it.id }
                ) { index ->

                    val callLog = callLogs[index] ?: return@items

                    CallLogItem(
                        callLog = callLog,
                        numberOfAppearance = 1,
                        onCallAction = onHandleCallActions,
                        onNavigate = onNavigate,
                        onDeleteCallLog = { onDeleteCallLogs(listOf(callLog.id)) },
                        modifier = Modifier.animateItem(),
                        sweetSelectState = sweetSelectState
                    )
                }
            }

        }
    }

}