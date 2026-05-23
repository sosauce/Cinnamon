@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class,
    ExperimentalHazeMaterialsApi::class
)

package com.sosauce.cinnamon.presentation.screens.messages

import android.provider.Telephony
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.conversation_settings.ConversationSettingActions
import com.sosauce.cinnamon.data.managers.ActiveThreadId
import com.sosauce.cinnamon.domain.model.CuteMessage
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.messages.components.TextingUnavailableBar
import com.sosauce.cinnamon.presentation.screens.messages.components.TextingUnavailableReason
import com.sosauce.cinnamon.presentation.screens.messages.components.bottombar.ConversationBottomBar
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.MessageLayout
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.MmsBubble
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.SandwichPosition
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.TextBubble
import com.sosauce.cinnamon.presentation.screens.messages.components.topbars.ConversationTopBar
import com.sosauce.cinnamon.presentation.screens.messages.components.topbars.SelectedTopBar
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.utils.SharedTransitionKeys
import com.sosauce.cinnamon.utils.bouncySpec
import com.sosauce.cinnamon.utils.isEmoji
import com.sosauce.sweetselect.rememberSweetSelectState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

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

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {
        val listState = rememberLazyListState()
        val sweetSelectState = rememberSweetSelectState<CuteMessage>()
        val lifecycleOwner = LocalLifecycleOwner.current

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

        LaunchedEffect(state.messages) { listState.animateScrollToItem(0) }

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
                    state.isShortCode -> TextingUnavailableBar(reason = TextingUnavailableReason.SHORT_CODE)
                    state.isSoloRecipientBlocked -> TextingUnavailableBar(reason = TextingUnavailableReason.BLOCKED)
                    else -> {
                        ConversationBottomBar(
                            conversationState = state,
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
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONVERSATION_WALLPAPER),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            renderInOverlayDuringTransition = false
                        )
                        .cloudy(
                            state.settings.wallpaperBlurIntensity,
                            state.settings.wallpaperBlurIntensity > 0
                        )
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
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
                                    val bubbleColor = when {
                                        message.body.isEmoji() || message.isScheduled -> Color.Transparent
                                        message.type == Telephony.Sms.MESSAGE_TYPE_INBOX -> MaterialTheme.colorScheme.tertiaryFixedDim
                                        else -> MaterialTheme.colorScheme.primaryFixedDim
                                    }
                                    var isTimestampVisible by remember { mutableStateOf(false) }

                                    MessageLayout(
                                        modifier = Modifier.animateItem(),
                                        message = message,
                                        sandwichPosition = sandwichPosition,
                                        isSelected = isSelected,
                                        isTimestampVisible = isTimestampVisible,
                                        recipients = state.nameOrBeautifiedRecipients,
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
                                                Telephony.Sms.MESSAGE_TYPE_OUTBOX -> {
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

                                                Telephony.Sms.MESSAGE_TYPE_FAILED -> {
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
                                            }
                                        }
                                    ) {
                                        if (message.isMms) {
                                            MmsBubble(
                                                message,
                                                sandwichPosition,
                                                bubbleColor,
                                                onHandleConversationActions
                                            )
                                        } else {
                                            TextBubble(
                                                message.body,
                                                message.type,
                                                sandwichPosition,
                                                message.isScheduled,
                                                bubbleColor
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