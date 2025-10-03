@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cuteconnect.ui.screens.messages

import android.net.Uri
import android.provider.Telephony.Sms
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sosauce.cuteconnect.data.actions.CommonAction
import com.sosauce.cuteconnect.domain.model.CuteMessage
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.ui.screens.messages.components.ConversationTopBar
import com.sosauce.cuteconnect.ui.screens.messages.components.MessageBubble
import com.sosauce.cuteconnect.ui.screens.messages.components.SelectedTopBar
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.utils.ImageUtils
import com.sosauce.cuteconnect.utils.cuteHazeEffect
import com.sosauce.cuteconnect.viewModels.ConversationViewModel
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import androidx.core.net.toUri
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.cloudy.cloudy
import com.sosauce.cuteconnect.data.actions.CallAction
import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingActions
import com.sosauce.cuteconnect.data.datastore.rememberDefaultSimCard
import com.sosauce.cuteconnect.domain.model.ConversationSettings
import com.sosauce.cuteconnect.domain.model.CuteContact
import com.sosauce.cuteconnect.domain.model.CuteConversation
import com.sosauce.cuteconnect.domain.model.CuteSimCard
import com.sosauce.cuteconnect.ui.navigation.LocalHazeState
import com.sosauce.cuteconnect.ui.screens.messages.components.ActionPicker
import com.sosauce.cuteconnect.ui.screens.messages.components.ConversationBottomBar
import com.sosauce.cuteconnect.ui.screens.messages.components.GenericThumbnail
import com.sosauce.cuteconnect.ui.screens.messages.components.MediaThumbnail
import com.sosauce.cuteconnect.ui.screens.messages.components.SandwichPosition
import com.sosauce.cuteconnect.ui.shared_components.SimSelector
import com.sosauce.cuteconnect.utils.addOrNot
import com.sosauce.cuteconnect.utils.addOrRemove
import com.sosauce.cuteconnect.utils.keyboardAsState
import com.sosauce.cuteconnect.utils.rememberHazeState
import com.sosauce.cuteconnect.utils.toReadableDate
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(
    cuteMessages: List<CuteMessage>,
    cuteConversation: CuteConversation,
    cuteSimCards: List<CuteSimCard>,
    threadId: Long,
    onHandleCommonAction: (CommonAction) -> Unit,
    onHandleCallAction: (CallAction) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val context = LocalContext.current
    val convoViewModel = koinViewModel<ConversationViewModel>(
        parameters = { parametersOf(threadId) }
    )
    val convoSettings by convoViewModel.settings.collectAsStateWithLifecycle()
    val selectedMessages = remember { mutableStateListOf<CuteMessage>() }
    val listState = rememberLazyListState()
    val chatWallpaperState = rememberHazeState()

    LaunchedEffect(cuteMessages) { listState.scrollToItem(listState.layoutInfo.totalItemsCount) }


    DynamicMaterialTheme(
        state = rememberDynamicMaterialThemeState(
            seedColor = if (convoSettings.color == 0) MaterialTheme.colorScheme.primary else Color(convoSettings.color),
            isDark = isSystemInDarkTheme() // NEED TO CHANGE THIS WHEN I HAVE APP THEME
        ),
    ) {

        Box {
            // Wallpaper
            AsyncImage(
                model = ImageUtils.imageRequester(convoSettings.wallpaper.toUri(), context),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(chatWallpaperState)
            )

            Scaffold(
                topBar = {
                    AnimatedContent(
                        targetState = selectedMessages.isEmpty()
                    ) {
                        if (it) {
                            ConversationTopBar(
                                cuteConversation = cuteConversation,
                                threadId = threadId,
                                onNavigateUp = onNavigateUp,
                                onHandleCallAction = onHandleCallAction,
                                onNavigate = onNavigate,
                                onDeleteConversation = {
                                    cuteMessages.fastForEach { message ->
                                        onHandleCommonAction(
                                            CommonAction.DeleteFromContentUri(
                                                Sms.CONTENT_URI,
                                                message.id
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth()
                            )
                        } else {
                            SelectedTopBar(
                                selectedCuteMessages = selectedMessages,
                                onUnselectAll = selectedMessages::clear,
                                onHandleCommonAction = onHandleCommonAction
                            )
                        }
                    }
                },
                bottomBar = {
                    ConversationBottomBar(
                        onSendMessage = { message ->
                            val cuteMessage = CuteMessage(
                                body = message,
                                type = Sms.MESSAGE_TYPE_SENT,
                                address = cuteConversation.recipients.first(),
                                threadId = threadId,
                            )
                            onHandleCommonAction(CommonAction.SendMessage(cuteMessage))
                        },
                        onSaveDraft = { draft ->
                            convoViewModel.handleConversationSettingsActions(
                                ConversationSettingActions.UpsertConversationSettings(
                                    convoSettings.copy(
                                        draft = draft
                                    )
                                )
                            )
                        },
                        cuteSimCards = cuteSimCards
                    )
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(LocalHazeState.current)
                        .hazeEffect(
                            state = chatWallpaperState
                        ) { blurRadius = convoSettings.wallpaperBlurIntensity.dp },
                    state = listState,
                    contentPadding = paddingValues
                ) {


                    cuteMessages.groupBy { it.date.toReadableDate() }.forEach { (date, cuteMessages) ->
                        item(
                            key = date
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CuteText(
                                    text = date,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.background,
                                            shape = CircleShape
                                        )
                                        .padding(5.dp)
                                )
                            }
                        }

                        itemsIndexed(
                            items = cuteMessages,
                            key = { _, cuteMessage -> cuteMessage.id }
                        ) { index, cuteMessage ->


                            val prev = cuteMessages.getOrNull(index - 1)
                            val next = cuteMessages.getOrNull(index + 1)
                            val sameAsPrev = prev?.type == cuteMessage.type
                            val sameAsNext = next?.type == cuteMessage.type

                            val sandwichPosition = when {
                                !sameAsPrev && !sameAsNext -> SandwichPosition.SOLO
                                !sameAsPrev && sameAsNext -> SandwichPosition.TOP
                                sameAsPrev && sameAsNext -> SandwichPosition.MIDDLE
                                sameAsPrev && !sameAsNext -> SandwichPosition.BOTTOM
                                else -> SandwichPosition.SOLO
                            }
                            MessageBubble(
                                modifier = Modifier.animateItem(),
                                cuteMessage = cuteMessage,
                                onAddMessageToSelected = { selectedMessages.addOrRemove(cuteMessage) },
                                isSelected = selectedMessages.contains(cuteMessage),
                                isInSelectMode = selectedMessages.isNotEmpty(),
                                sandwichPosition = sandwichPosition,
                                allMedias = emptyList(),
                                onHandleCommonAction = onHandleCommonAction
                            )

                        }
                    }
                }
            }
        }
    }
}