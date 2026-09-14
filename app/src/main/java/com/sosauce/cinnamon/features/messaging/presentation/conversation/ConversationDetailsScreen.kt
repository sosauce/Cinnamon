@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class,
    ExperimentalHazeMaterialsApi::class, ExperimentalZoomableApi::class
)

package com.sosauce.cinnamon.features.messaging.presentation.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.sky
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingActions
import com.sosauce.cinnamon.core.telephony.message.ActiveThreadId
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.datastore.rememberChatZoomScale
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.TextBubble
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars.ConversationTopBar
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.cinnamon.core.utils.isEmoji
import com.sosauce.cinnamon.features.messaging.domain.CuteMessage
import com.sosauce.cinnamon.features.messaging.domain.MessageType
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.TextingUnavailableBar
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.TextingUnavailableReason
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.ConversationBottomBar
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.MessageLayout
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.MmsBubble
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bubble.SandwichPosition
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.topbars.SelectedTopBar
import com.sosauce.nekobites.animations.unclippedContentTransform
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.sweetselect.rememberSweetSelectState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomableWithScroll

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.ConversationDetailsScreen(
    state: ConversationDetailsState,
    prefilledMessage: String,
    onHandleCallAction: (CallAction) -> Unit,
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
    val transformState = rememberTransformableState { _, zoomChange, _, _ ->
        chatFontScale = (chatFontScale * zoomChange).coerceIn(0.9f, 2.2f)
    }

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
                    state.conversation.threadId

                else -> ActiveThreadId.threadId = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onRetire { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.messages) { listState.animateScrollToItem(0) }

    LoadingBox(
        isLoading = state.isLoading
    ) {
        Scaffold(
            topBar = {
                AnimatedContent(
                    targetState = sweetSelectState.isInSelectionMode,
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = slideInVertically(bouncySpec()) { -it } + fadeIn(),
                            initialContentExit = slideOutVertically(bouncySpec()) { -it } + fadeOut(),
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                ) {
                    if (it) {
                        SelectedTopBar(
                            sweetSelectState = sweetSelectState,
                            onSelectAll = { sweetSelectState.toggleAll(state.messages.values.flatten()) },
                            onUnselectAll = sweetSelectState::clearSelected,
                            onHandleConversationActions = onHandleConversationActions
                        )
                    } else {
                        ConversationTopBar(
                            state = state,
                            onNavigateUp = onNavigateUp,
                            onHandleCallAction = onHandleCallAction,
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
                    state.conversation.participants.firstOrNull()?.isBlocked == true -> TextingUnavailableBar(
                        reason = TextingUnavailableReason.BLOCKED
                    )
                    else -> {
                        ConversationBottomBar(
                            state = state,
                            prefilledMessage = prefilledMessage,
                            onSaveDraft = { draft ->
                                onHandleConversationSettingsActions(
                                    ConversationSettingActions.UpsertConversationSettings(
                                        state.settings.copy(
                                            draft = draft
                                        )
                                    )
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
                // Wallpaper
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
                            .transformable(transformState),
                        state = listState,
                        contentPadding = paddingValues,
                        reverseLayout = true
                    ) {


                        if (state.messages.isNotEmpty()) {
                            state.messages
                                .forEach { (date, messages) ->

                                    itemsIndexed(
                                        items = messages,
                                        key = { _, message ->
                                            val type = when {
                                                message.isScheduled -> "scheduled"
                                                message.isMms -> "mms"
                                                else -> "sms"
                                            }
                                            "${type}_${message.id}"
                                        },
                                        contentType = { _, message -> message.isMms }
                                    ) { index, message ->

                                        val prev = messages.getOrNull(index + 1)
                                        val next = messages.getOrNull(index - 1)
                                        val sameAsPrev = prev?.type == message.type
                                        val sameAsNext = next?.type == message.type

                                        val sandwichPosition = when {
                                            !sameAsPrev && !sameAsNext -> SandwichPosition.SOLO
                                            !sameAsPrev && sameAsNext -> SandwichPosition.TOP
                                            sameAsPrev && sameAsNext -> SandwichPosition.MIDDLE
                                            sameAsPrev && !sameAsNext -> SandwichPosition.BOTTOM
                                            else -> SandwichPosition.SOLO
                                        }

                                        val isSelected by sweetSelectState.isSelectedAsState(message)
                                        var isTimestampVisible by remember { mutableStateOf(false) }

                                        MessageLayout(
                                            modifier = Modifier.animateItem(),
                                            message = message,
                                            sandwichPosition = sandwichPosition,
                                            isSelected = isSelected,
                                            isGroupChat = state.conversation.isGroupChat,
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
                                                when (message.type) {
                                                    MessageType.SENDING -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            CircularWavyProgressIndicator(
                                                                modifier = Modifier.size(30.dp)
                                                            )
                                                            Spacer(Modifier.width(5.dp))
                                                            Text(
                                                                text = stringResource(R.string.sending)
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
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                            Spacer(Modifier.width(5.dp))
                                                            Text(
                                                                text = stringResource(R.string.not_sent),
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }

                                                    else -> Unit
                                                }
                                            }
                                        ) {

                                            val bubbleColor = when {
                                                message.body.isEmoji() || message.isScheduled -> Color.Transparent
                                                message.type == MessageType.RECEIVED -> MaterialTheme.colorScheme.tertiaryFixedDim
                                                else -> MaterialTheme.colorScheme.primaryFixedDim
                                            }
                                            if (message.isMms) {
                                                MmsBubble(
                                                    message = message,
                                                    sandwichPosition = sandwichPosition,
                                                    bubbleColor = bubbleColor,
                                                    onHandleConversationActions = onHandleConversationActions
                                                )
                                            } else {
                                                TextBubble(
                                                    body = message.body,
                                                    type = message.type,
                                                    sandwichPosition = sandwichPosition,
                                                    isScheduled = message.isScheduled,
                                                    bubbleColor = bubbleColor
                                                )
                                            }
                                        }
                                    }
                                    item(
                                        key = date,
                                        contentType = "date_header"
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .animateItem()
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = date,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(50)
                                                    )
                                                    .padding(5.dp)
                                            )
                                        }
                                    }
                                }
                        } else {
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.conversation_starter),
                                        style = MaterialTheme.typography.headlineMediumEmphasized.copy(
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.conversation_starter_desc),
                                        style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }

                    }
                }
            }

        }
    }
}