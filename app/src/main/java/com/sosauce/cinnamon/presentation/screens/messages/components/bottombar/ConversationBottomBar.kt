@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components.bottombar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberShowCharCount
import com.sosauce.cinnamon.data.providers.ComposeFileProvider
import com.sosauce.cinnamon.data.schedulers.scheduled_messages.ScheduledMessage
import com.sosauce.cinnamon.presentation.screens.messages.ConversationActions
import com.sosauce.cinnamon.presentation.screens.messages.ConversationDetailsState
import com.sosauce.cinnamon.presentation.screens.messages.components.AnimatedCounter
import com.sosauce.cinnamon.presentation.screens.messages.components.dialogs.ScheduleMessageDialog
import com.sosauce.cinnamon.utils.bouncySpec
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import com.sosauce.cinnamon.utils.toDateAndTime
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.ceil
import androidx.compose.ui.layout.onSizeChanged


@Composable
fun ConversationBottomBar(
    conversationState: ConversationDetailsState,
    onSaveDraft: (String) -> Unit,
    prefilledMessage: String,
    onHandleConversationActions: (ConversationActions) -> Unit
) {
    val viewModel = koinViewModel<BottomBarViewModel>(
        parameters = { parametersOf(conversationState.threadId, prefilledMessage) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val showCharCount by rememberShowCharCount()

    var showSchedulerDialog by remember { mutableStateOf(false) }
    var isActionPickerExpanded by remember { mutableStateOf(false) }
    var cameraPicture by remember { mutableStateOf<Uri?>(null) }


    val mediaSelector =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            viewModel.addAttachments(uris)
        }
    val systemCameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            val uri = cameraPicture ?: return@rememberLauncherForActivityResult
            viewModel.addAttachments(listOf(uri))
            cameraPicture = null
        }
    val documentsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            viewModel.addAttachments(uris)
        }
    val attachmentRotation by animateFloatAsState(
        targetValue = if (isActionPickerExpanded) 45f else 0f
    )
    val attachmentOptions = listOf(
        MoreOptions(
            onClick = {
                isActionPickerExpanded = false
                val uri = ComposeFileProvider.getImageUri(context)
                cameraPicture = uri
                systemCameraLauncher.launch(uri)
            },
            icon = R.drawable.camera,
            text = R.string.camera
        ),
        MoreOptions(
            onClick = {
                mediaSelector.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
                isActionPickerExpanded = false
            },
            icon = R.drawable.gallery,
            text = R.string.gallery
        ),
        MoreOptions(
            onClick = {
                documentsLauncher.launch(arrayOf("*/*"))
                isActionPickerExpanded = false
            },
            icon = R.drawable.attach_file,
            text = R.string.attach_file
        ),
        MoreOptions(
            onClick = {
                showSchedulerDialog = true
                isActionPickerExpanded = false
            },
            icon = R.drawable.schedule,
            text = R.string.schedule_message
        )
    )

    RetainedEffect(Unit) {
        onRetire { onSaveDraft(state.message) }
    }

    if (showSchedulerDialog) {
        ScheduleMessageDialog(
            onDismissRequest = { showSchedulerDialog = false },
            onSetScheduledTime = viewModel::setScheduledTime
        )
    }

    Column(
        modifier = Modifier
            .selfAlignHorizontally()
            .fillMaxWidth(0.95f)
    ) {
        Popup(
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Below
            )
        ) {
            FloatingActionButtonMenu(
                expanded = isActionPickerExpanded,
                button = {},
                horizontalAlignment = Alignment.Start
            ) {
                attachmentOptions.fastForEach { option ->
                    FloatingActionButtonMenuItem(
                        onClick = option.onClick,
                        text = { Text(stringResource(option.text)) },
                        icon = {
                            Icon(
                                painter = painterResource(option.icon),
                                contentDescription = null
                            )
                        }
                    )
                }

            }
        }

        AnimatedVisibility(
            visible = state.scheduledTime != null,
            enter = slideInVertically(bouncySpec()) { it } + fadeIn(),
            exit = slideOutVertically(bouncySpec()) { it } + fadeOut(),
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateContentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .dashedBorder(RoundedCornerShape(12.dp))
                    .padding(5.dp)
            ) {
                Text(state.scheduledTime?.toDateAndTime() ?: "")
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = viewModel::removeScheduledTime,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = state.attachments.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            enter = slideInVertically(bouncySpec()) { it } + fadeIn(),
            exit = slideOutVertically(bouncySpec()) { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .animateContentSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                ) {
                    items(
                        items = state.attachments,
                        key = { it }
                    ) { attachment ->
                        ReadyToSendAttachment(
                            modifier = Modifier.animateItem(),
                            attachment = attachment,
                            onRemoveAttachment = { viewModel.removeAttachment(attachment) }
                        )
                    }
                }
            }
        }
        val density = LocalDensity.current
        var barHeight by remember { mutableStateOf(0.dp) }
        val cornerRadius = (barHeight / 2).coerceAtMost(28.dp)

        Column(
            modifier = Modifier
                .imePadding()
                .navigationBarsPadding()
                .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(cornerRadius),
                )
                .padding(10.dp)
        ){
            Row(
                modifier = Modifier.animateContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isActionPickerExpanded = !isActionPickerExpanded },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        modifier = Modifier.rotate(attachmentRotation)
                    )
                }
                TextField(
                    state = viewModel.textFieldState,
                    shape = RoundedCornerShape(cornerRadius),
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 6),
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    trailingIcon =
                        if (showCharCount) {
                            {
                                val charCount = state.message.length
                                val segmentCount =
                                    if (charCount <= 160) 1 else ceil(charCount / 160.0).toInt()
                                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmallEmphasized) {
                                    Row {
                                        AnimatedCounter(charCount)
                                        Text("/")
                                        AnimatedCounter(segmentCount)
                                    }
                                }
                            }

                        } else null
                )
                IconButton(
                    onClick = {
                        if (state.scheduledTime != null) {
                            onHandleConversationActions(
                                ConversationActions.ScheduleMessage(
                                    ScheduledMessage(
                                        threadId = conversationState.threadId,
                                        address = conversationState.recipients.first(),
                                        message = state.message,
                                        sendAt = state.scheduledTime ?: return@IconButton
                                    )
                                )
                            )
                        } else {
                            onHandleConversationActions(
                                ConversationActions.SendMessage(
                                    addresses = conversationState.recipients,
                                    message = state.message,
                                    attachments = state.attachments
                                )
                            )
                        }
                        viewModel.resetState()
                    },
                    shapes = IconButtonDefaults.shapes(),
                    enabled = (state.message.isNotEmpty() && state.message.isNotBlank()) || state.attachments.isNotEmpty(),
                ) {

                    AnimatedContent(
                        targetState = state.scheduledTime != null,
                        transitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } }
                    ) {
                        if (it) {
                            Icon(
                                painter = painterResource(R.drawable.schedule_send),
                                contentDescription = "send scheduled message"
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.send_filled),
                                contentDescription = "send message"
                            )
                        }

                    }
                }
            }
        }
    }
}

data class MoreOptions(
    val onClick: () -> Unit,
    val icon: Int,
    val text: Int,
    val tint: Color? = null
)

@Composable
fun Modifier.dashedBorder(
    shape: Shape,
    animate: Boolean = true
): Modifier {

    val infiniteTransition = rememberInfiniteTransition()
    val density = LocalDensity.current
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = with(density) { 40.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        )
    )
    val color = MaterialTheme.colorScheme.onSurfaceVariant

    return drawWithContent {
        val outline = shape.createOutline(size, layoutDirection, this)
        val dashStroke = Stroke(
            cap = StrokeCap.Round,
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(10.dp.toPx(), 10.dp.toPx()),
                phase = if (animate) phase else 0f
            )
        )
        drawContent()
        drawOutline(
            outline = outline,
            color = color,
            style = dashStroke
        )
    }
}
