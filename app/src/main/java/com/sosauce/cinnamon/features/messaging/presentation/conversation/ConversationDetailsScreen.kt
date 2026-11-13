@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class,
    ExperimentalHazeMaterialsApi::class, ExperimentalZoomableApi::class
)

package com.sosauce.cinnamon.features.messaging.presentation.conversation

import com.sosauce.cinnamon.core.ui.components.CuteHero
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sosauce.cinnamon.features.messaging.domain.listKey
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars.ConversationSearchTopBar
import com.sosauce.sweetselect.SweetSelectState
import kotlin.math.abs
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.datastore.rememberChatZoomScale
import com.sosauce.cinnamon.core.datastore.rememberEnablePinchToZoom
import com.sosauce.cinnamon.core.telephony.message.ActiveThreadId
import com.sosauce.cinnamon.core.utils.isEmoji
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingActions
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.domain.MessageType
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.TextingUnavailableBar
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.TextingUnavailableReason
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.ConversationBottomBar
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.MessageLayout
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.MmsBubble
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.SandwichPosition
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.TextBubble
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars.ConversationTopBar
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars.SelectedTopBar
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.sweetselect.rememberSweetSelectState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import net.engawapg.lib.zoomable.ExperimentalZoomableApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.ConversationDetailsScreen(
    state: ConversationDetailsState,
    messages: LazyPagingItems<CuteMessage>,
    searchTextFieldState: TextFieldState,
    prefilledMessage: String,
    onDeleteConversation: () -> Unit,
    onHandleConversationSettingsActions: (ConversationSettingActions) -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigate: (Screen) -> Unit
) {

    val listState = rememberLazyListState()
    val sweetSelectState = rememberSweetSelectState<CuteMessage>()
    val lifecycleOwner = LocalLifecycleOwner.current
    var chatFontScale by rememberChatZoomScale()
    val pinchToZoom by rememberEnablePinchToZoom()
    val transformState = rememberTransformableState { _, zoomChange, _, _ ->
        chatFontScale = (chatFontScale * zoomChange).coerceIn(0.9f, 2.2f)
    }
    val isGroupChat = state.participants.size > 1

    val density = LocalDensity.current
    val scaledDensity = remember(density, chatFontScale) {
        Density(
            density = density.density,
            fontScale = density.fontScale * chatFontScale
        )
    }


    RetainedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> ActiveThreadId.threadId =
                    state.threadId

                else -> ActiveThreadId.threadId = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onRetire { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scheduledCount = state.scheduledMessages.size
    val totalCount = scheduledCount + messages.itemCount

    /**
     * Message at [listIndex] of the conversation list without triggering a page load,
     * null if out of bounds or not loaded yet.
     */
    fun messageAt(listIndex: Int): CuteMessage? = when {
        listIndex < 0 || listIndex >= totalCount -> null
        listIndex < scheduledCount -> state.scheduledMessages[listIndex]
        else -> messages.peek(listIndex - scheduledCount)
    }

    val search = state.search
    val highlightQuery = search.query.takeIf { search.isActive && it.isNotBlank() }
    val focusedMatchKey = search.currentMatch?.key

    // Scroll to the bottom when a new message shows up (sent, received or scheduled).
    // Ignores the key going null -> non-null, which happens when pages get dropped by a search jump.
    val newestMessageKey = messageAt(0)?.listKey
    var lastNewestMessageKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(newestMessageKey) {
        if (newestMessageKey == null) return@LaunchedEffect
        if (
            lastNewestMessageKey != null &&
            lastNewestMessageKey != newestMessageKey &&
            !search.isActive
        ) {
            listState.animateScrollToItem(0)
        }
        lastNewestMessageKey = newestMessageKey
    }

    // Bring the focused search match into view, unloaded pages get loaded once we land on them
    LaunchedEffect(search.currentMatch) {
        val match = search.currentMatch ?: return@LaunchedEffect
        val distance = abs(listState.firstVisibleItemIndex - match.listIndex)
        if (distance > SEARCH_ANIMATED_SCROLL_MAX_DISTANCE) {
            listState.scrollToItem(match.listIndex)
        } else {
            listState.animateScrollToItem(match.listIndex)
        }
    }

    BackHandler(enabled = search.isActive && !sweetSelectState.isInSelectionMode) {
        onHandleConversationActions(ConversationActions.CloseSearch)
    }

    val topBarMode = when {
        sweetSelectState.isInSelectionMode -> TopBarMode.SELECTION
        search.isActive -> TopBarMode.SEARCH
        else -> TopBarMode.DEFAULT
    }

    LoadingBox(
        isLoading = state.isLoading
    ) {
        Scaffold(
            topBar = {
                AnimatedContent(
                    targetState = topBarMode,
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = slideInVertically(bouncySpec()) { -it } + fadeIn(),
                            initialContentExit = slideOutVertically(bouncySpec()) { -it } + fadeOut(),
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                ) { mode ->
                    when (mode) {
                        TopBarMode.SELECTION -> SelectedTopBar(
                            sweetSelectState = sweetSelectState,
                            onSelectAll = {
                                // only what's loaded, see paging
                                sweetSelectState.toggleAll(
                                    state.scheduledMessages + messages.itemSnapshotList.items
                                )
                            },
                            onUnselectAll = sweetSelectState::clearSelected,
                            onNavigate = onNavigate,
                            onHandleConversationActions = onHandleConversationActions
                        )

                        TopBarMode.SEARCH -> ConversationSearchTopBar(
                            textFieldState = searchTextFieldState,
                            search = search,
                            onHandleConversationActions = onHandleConversationActions
                        )

                        TopBarMode.DEFAULT -> ConversationTopBar(
                            state = state,
                            onNavigateUp = onNavigateUp,
                            onNavigate = onNavigate,
                            onDeleteConversation = onDeleteConversation,
                            onHandleConversationActions = onHandleConversationActions
                        )
                    }
                }
            },
            bottomBar = {
                when {
                    state.isShortCode -> TextingUnavailableBar(
                        reason = TextingUnavailableReason.SHORT_CODE
                    )
                    state.participants.firstOrNull()?.isBlocked == true -> TextingUnavailableBar(
                        reason = TextingUnavailableReason.BLOCKED
                    )
                    else -> {
                        ConversationBottomBar(
                            state = state,
                            prefilledMessage = prefilledMessage,
                            onSaveDraft = { draft ->
                                onHandleConversationSettingsActions(
                                    ConversationSettingActions.UpsertDraft(draft)
                                )
                            },
                            onHandleConversationActions = onHandleConversationActions
                        )
                    }
                }
            }
        ) { paddingValues ->

            Box(
                Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = state.settings.wallpaper,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .cloudy(
                            state.settings.wallpaperBlurIntensity,
                            state.settings.wallpaperBlurIntensity > 0
                        )
                )
                CompositionLocalProvider(
                    LocalDensity provides scaledDensity
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(
                                state = transformState,
                                enabled = pinchToZoom
                            ),
                        state = listState,
                        contentPadding = paddingValues,
                        reverseLayout = true
                    ) {


                        // List order (reversed layout, index 0 = bottom/newest):
                        // scheduled messages first, then the paged SMS/MMS.
                        // Date headers are drawn inside the oldest message of each day so that
                        // list indexes always map 1:1 to messages (needed to jump to search results).

                        val messageItem: @Composable LazyItemScope.(Int, CuteMessage) -> Unit = { listIndex, message ->
                            val older = messageAt(listIndex + 1)
                            val newer = messageAt(listIndex - 1)
                            val isOldest = listIndex == totalCount - 1

                            ConversationMessageItem(
                                message = message,
                                older = older,
                                newer = newer,
                                showDateHeader = older?.let { it.date != message.date } ?: isOldest,
                                sweetSelectState = sweetSelectState,
                                isGroupChat = isGroupChat,
                                highlightQuery = highlightQuery,
                                isFocusedMatch = focusedMatchKey == message.listKey,
                                onHandleConversationActions = onHandleConversationActions
                            )
                        }

                        itemsIndexed(
                            items = state.scheduledMessages,
                            key = { _, message -> message.listKey },
                            contentType = { _, _ -> "scheduled" }
                        ) { index, message ->
                            messageItem(index, message)
                        }

                        items(
                            count = messages.itemCount,
                            key = messages.itemKey { it.listKey },
                            contentType = messages.itemContentType { it.isMms }
                        ) { index ->
                            val message = messages[index]
                            if (message != null) {
                                messageItem(scheduledCount + index, message)
                            } else {
                                // placeholder while the page loads
                                Spacer(Modifier.fillMaxWidth().height(56.dp))
                            }
                        }

                        if (
                            totalCount == 0 &&
                            messages.loadState.refresh is LoadState.NotLoading
                        ) {
                            item {
                                CuteHero(
                                    icon = R.drawable.message_rounded,
                                    title = stringResource(R.string.conversation_starter),
                                    description = stringResource(R.string.conversation_starter_desc),
                                    size = 140.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                )
                            }
                        }

                    }
                }
            }

        }
    }
}
private enum class TopBarMode { DEFAULT, SELECTION, SEARCH }

// Past this many items a smooth scroll gets slow, so we jump instead
private const val SEARCH_ANIMATED_SCROLL_MAX_DISTANCE = 40

@Composable
private fun LazyItemScope.ConversationMessageItem(
    message: CuteMessage,
    older: CuteMessage?,
    newer: CuteMessage?,
    showDateHeader: Boolean,
    sweetSelectState: SweetSelectState<CuteMessage>,
    isGroupChat: Boolean,
    highlightQuery: String?,
    isFocusedMatch: Boolean,
    onHandleConversationActions: (ConversationActions) -> Unit
) {
    // bubbles only connect with neighbours from the same day and of the same type
    val sameAsPrev = older != null && older.date == message.date && older.type == message.type
    val sameAsNext = newer != null && newer.date == message.date && newer.type == message.type

    val sandwichPosition = when {
        !sameAsPrev && !sameAsNext -> SandwichPosition.SOLO
        !sameAsPrev && sameAsNext -> SandwichPosition.TOP
        sameAsPrev && sameAsNext -> SandwichPosition.MIDDLE
        sameAsPrev && !sameAsNext -> SandwichPosition.BOTTOM
        else -> SandwichPosition.SOLO
    }

    val isSelected by sweetSelectState.isSelectedAsState(message)
    var isTimestampVisible by remember { mutableStateOf(false) }

    Column(Modifier.animateItem()) {
        if (showDateHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = message.date,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        MessageLayout(
            message = message,
            sandwichPosition = sandwichPosition,
            isSelected = isSelected,
            isGroupChat = isGroupChat,
            isTimestampVisible = isTimestampVisible,
            onClick = {
                if (sweetSelectState.isInSelectionMode) {
                    sweetSelectState.toggle(message)
                } else {
                    isTimestampVisible = !isTimestampVisible
                }
            },
            onLongClick = { sweetSelectState.toggle(message) },
            statusContent = {
                AnimatedContent(
                    targetState = message.type
                ) {
                    when (it) {
                        MessageType.SENDING -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.sending),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        MessageType.FAILED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.not_sent),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        else -> Unit
                    }
                }

            }
        ) {

            val bubbleColor = when {
                message.body.isEmoji() || message.isScheduled -> Color.Transparent
                message.type == MessageType.RECEIVED -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.primary
            }
            if (message.isMms) {
                MmsBubble(
                    message = message,
                    sandwichPosition = sandwichPosition,
                    bubbleColor = bubbleColor,
                    onHandleConversationActions = onHandleConversationActions,
                    highlightQuery = highlightQuery,
                    isFocusedMatch = isFocusedMatch
                )
            } else {
                TextBubble(
                    body = message.body,
                    type = message.type,
                    sandwichPosition = sandwichPosition,
                    isScheduled = message.isScheduled,
                    bubbleColor = bubbleColor,
                    highlightQuery = highlightQuery,
                    isFocusedMatch = isFocusedMatch
                )
            }
        }
    }
}
