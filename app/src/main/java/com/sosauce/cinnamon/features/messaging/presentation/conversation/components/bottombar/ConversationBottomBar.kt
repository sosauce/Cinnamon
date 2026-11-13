@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar

import com.sosauce.nekobites.animations.rememberAnimatedShape
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import android.net.Uri
import android.telephony.SmsMessage
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.providers.ComposeFileProvider
import com.sosauce.cinnamon.core.datastore.rememberShowCharCount
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.core.utils.toDateAndTime
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessageEntity
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsState
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.dialogs.ScheduleMessageDialog
import com.sosauce.nekobites.animations.AnimatedCounter
import com.sosauce.nekobites.animations.bouncySpec
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ConversationBottomBar(
    state: ConversationDetailsState,
    onSaveDraft: (String) -> Unit,
    prefilledMessage: String,
    onHandleConversationActions: (ConversationActions) -> Unit
) {
    val viewModel = koinViewModel<BottomBarViewModel>(
        parameters = { parametersOf(state.threadId, prefilledMessage) }
    )
    val bottomBarState by viewModel.state.collectAsStateWithLifecycle()

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
                if (bottomBarState.attachments.isEmpty()) showSchedulerDialog = true
                isActionPickerExpanded = false
            },
            icon = R.drawable.schedule,
            text = R.string.schedule_message
        )
    )

    RetainedEffect(Unit) {
        onRetire { onSaveDraft(bottomBarState.message) }
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
        ScheduledTimeBanner(
            scheduledTime = bottomBarState.scheduledTime,
            onRemoveScheduledTime = viewModel::removeScheduledTime
        )
        AttachmentsBox(
            attachments = bottomBarState.attachments,
            onRemoveAttachment = viewModel::removeAttachment
        )

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
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.animateContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachmentPicker(
                    expanded = isActionPickerExpanded,
                    options = attachmentOptions,
                    onToggle = { isActionPickerExpanded = !isActionPickerExpanded }
                )
                MessageTextField(
                    textFieldState = viewModel.textFieldState,
                    message = bottomBarState.message,
                    cornerRadius = cornerRadius,
                    showCharCount = showCharCount,
                    modifier = Modifier.weight(1f)
                )


                SendButton(
                    isScheduled = bottomBarState.scheduledTime != null,
                    enabled = (bottomBarState.message.isNotEmpty() && bottomBarState.message.isNotBlank()) || bottomBarState.attachments.isNotEmpty(),
                    onClick = send@{
                        if (bottomBarState.scheduledTime != null && bottomBarState.attachments.isEmpty()) {
                            onHandleConversationActions(
                                ConversationActions.ScheduleMessage(
                                    ScheduledMessageEntity(
                                        threadId = state.threadId,
                                        addresses = state.participants.fastMap { it.rawNumber },
                                        message = bottomBarState.message,
                                        sendAt = bottomBarState.scheduledTime ?: return@send
                                    )
                                )
                            )
                        } else {
                            onHandleConversationActions(
                                ConversationActions.SendMessage(
                                    addresses = state.participants.fastMap { it.rawNumber },
                                    message = bottomBarState.message,
                                    attachments = bottomBarState.attachments
                                )
                            )
                        }
                        viewModel.resetState()
                    }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ScheduledTimeBanner(
    scheduledTime: Long?,
    onRemoveScheduledTime: () -> Unit
) {
    AnimatedVisibility(
        visible = scheduledTime != null,
        enter = slideInVertically(bouncySpec()) { it } + fadeIn(),
        exit = slideOutVertically(bouncySpec()) { it } + fadeOut(),
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .animateContentSize()

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .dashedBorder(CircleShape)
                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.schedule),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = scheduledTime?.toDateAndTime() ?: "",
                style = MaterialTheme.typography.labelLargeEmphasized
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onRemoveScheduledTime,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.AttachmentsBox(
    attachments: List<Uri>,
    onRemoveAttachment: (Uri) -> Unit
) {
    AnimatedVisibility(
        visible = attachments.isNotEmpty(),
        enter = slideInVertically(bouncySpec()) { it } + fadeIn(),
        exit = slideOutVertically(bouncySpec()) { it } + fadeOut(),
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .animateContentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(4.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = attachments,
                    key = { _, attachment -> attachment }
                ) { index, attachment ->
                    ReadyToSendAttachment(
                        modifier = Modifier.animateItem(
                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                            fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                        ),
                        attachment = attachment,
                        shape = animatedAttachmentShape(
                            isFirst = index == 0,
                            isLast = index == attachments.lastIndex
                        ),
                        onRemoveAttachment = { onRemoveAttachment(attachment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPicker(
    expanded: Boolean,
    options: List<MoreOptions>,
    onToggle: () -> Unit
) {
    val attachmentRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f
    )
    val attachContainer by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    )
    val attachShape = rememberAnimatedShape(
        condition = expanded,
        shapeA = MaterialShapes.Circle,
        shapeB = MaterialShapes.Cookie9Sided
    )

    Column {
        Popup(
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Below
            )
        ) {
            FloatingActionButtonMenu(
                expanded = expanded,
                button = {},
                horizontalAlignment = Alignment.Start
            ) {
                options.fastForEach { option ->
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

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(attachShape)
                .background(attachContainer)
                .clickable(onClick = onToggle)
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
                tint = contentColorFor(attachContainer),
                modifier = Modifier.graphicsLayer { rotationZ = attachmentRotation }
            )
        }
    }
}

@Composable
private fun MessageTextField(
    textFieldState: TextFieldState,
    message: String,
    cornerRadius: Dp,
    showCharCount: Boolean,
    modifier: Modifier = Modifier
) {
    TextField(
        state = textFieldState,
        shape = RoundedCornerShape(cornerRadius),
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = 1,
            maxHeightInLines = 6
        ),
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.type_a_message),
                maxLines = 1
            )
        },
        trailingIcon =
            if (showCharCount) {
                { CharCounter(message) }
            } else null
    )
}

@Composable
private fun CharCounter(message: String) {
    val charCount = message.length
    val segmentCount = SmsMessage.calculateLength(message, false)[0]
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmallEmphasized) {
        Row {
           AnimatedCounter(count = charCount, textStyle = LocalTextStyle.current)
            Text("/")
           AnimatedCounter(count = segmentCount, textStyle = LocalTextStyle.current)
        }
    }
}

@Composable
private fun SendButton(
    isScheduled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        enabled = enabled,
        modifier = Modifier.size(48.dp)
    ) {

        AnimatedContent(
            targetState = isScheduled,
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

    val density = LocalDensity.current
    val phase = if (animate) {
        rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = with(density) { 40.dp.toPx() },
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing)
            )
        )
    } else null
    val color = MaterialTheme.colorScheme.onSurfaceVariant

    return drawWithContent {
        val outline = shape.createOutline(size, layoutDirection, this)
        val dashStroke = Stroke(
            cap = StrokeCap.Round,
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(10.dp.toPx(), 10.dp.toPx()),
                phase = phase?.value ?: 0f
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
