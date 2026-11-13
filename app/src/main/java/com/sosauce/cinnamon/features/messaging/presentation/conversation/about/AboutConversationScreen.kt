@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.DefaultGroupChatIcon
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.core.ui.components.dialogs.BlockUnblockDialog
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.items.CuteListItemDefaults
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.cinnamon.features.messaging.data.model.CuteAttachment
import com.sosauce.cinnamon.features.messaging.domain.Participant
import com.sosauce.cinnamon.features.messaging.domain.isGroupChat
import com.sosauce.cinnamon.features.messaging.domain.name
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationActions
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsState
import com.sosauce.nekobites.components.LoadingBox

@Composable
fun AboutConversationScreen(
    state: ConversationDetailsState,
    medias: List<CuteAttachment.AttachmentDetails>,
    onNavigateBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteConversation: () -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
) {

    val soloParticipant = state.participants.singleOrNull()
    var showBlockDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    val name = state.participants.name()

    if (showBlockDialog && soloParticipant != null) {
        BlockUnblockDialog(
            displayName = soloParticipant.displayName,
            isBlocked = soloParticipant.isBlocked,
            onDismissRequest = { showBlockDialog = false },
            onConfirm = {
                onHandleConversationActions(ConversationActions.ToggleBlock)
                showBlockDialog = false
            }
        )
    }

    viewerIndex?.let { index ->
        MediaViewerDialog(
            medias = medias,
            initialIndex = index.coerceIn(0, (medias.size - 1).coerceAtLeast(0)),
            onDismissRequest = { viewerIndex = null },
            onHandleConversationActions = onHandleConversationActions
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConversation()
                    },
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(R.string.delete)) }
            },
            text = { Text(stringResource(R.string.delete_convo_u_sure)) },
            title = { Text(stringResource(R.string.delete_convo)) },
        )
    }

    LoadingBox(
        isLoading = state.isLoading
    ) {
        Scaffold(
            bottomBar = { CuteNavigationButton(onNavigateUp = onNavigateBack) }
        ) { pv ->
            val posterHeight = 280.dp
            val iconSize = 170.dp
            val cardOverlap = 25.dp
            val seamY = posterHeight - cardOverlap
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(bottom = pv.calculateBottomPadding()))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(posterHeight + 20.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        )
                        AsyncImage(
                            model = state.settings.wallpaper,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .cloudy(30),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = seamY)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(top = iconSize / 2 + 12.dp)
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineLargeEmphasized,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            if (state.participants.isGroupChat()) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.participants_count,
                                        state.participants.size,
                                        state.participants.size
                                    ),
                                    style = MaterialTheme.typography.bodyMediumEmphasized,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                            if (soloParticipant != null) {
                                Spacer(Modifier.height(15.dp))
                                AboutConversationActionsRow(
                                    threadId = state.threadId,
                                    participant = soloParticipant,
                                    onNavigate = onNavigate,
                                    onHandleConversationActions = onHandleConversationActions
                                )
                            }
                            Spacer(Modifier.height(25.dp))
                            if (medias.isNotEmpty()) {
                                SectionTitle(
                                    text = "${
                                        pluralStringResource(R.plurals.medias_plural, medias.size)
                                    }: ${medias.size}"
                                )
                                Card(
                                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    HorizontalMultiBrowseCarousel(
                                        state = rememberCarouselState { medias.size },
                                        preferredItemWidth = 186.dp,
                                        itemSpacing = 5.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                    ) { index ->
                                        MediaThumbnail(
                                            media = medias[index],
                                            onClick = { viewerIndex = index },
                                            modifier = Modifier
                                                .height(205.dp)
                                                .maskClip(MaterialTheme.shapes.extraLarge)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(25.dp))
                            }
                            if (state.participants.isGroupChat()) {
                                SectionTitle(text = stringResource(R.string.participants))
                                state.participants.fastForEachIndexed { index, participant ->
                                    CuteListItem(
                                        onClick = { onNavigate(participant.contactScreen()) },
                                        shape = CuteListItemDefaults.getItemShape(
                                            index,
                                            state.participants.size
                                        ),
                                        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                        leadingContent = {
                                            DefaultContactIcon(
                                                firstLetter = participant.displayName.firstOrNull(),
                                                contactPfp = participant.photoUri,
                                                size = 40.dp,
                                                modifier = Modifier.padding(start = 10.dp)
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = participant.displayName,
                                            style = MaterialTheme.typography.bodyLargeEmphasized,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        val beautifiedNumber = participant.rawNumber.beautifyNumber()
                                        if (beautifiedNumber != participant.displayName) {
                                            Text(
                                                text = beautifiedNumber,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(25.dp))
                            }
                            val dangerActions = buildList {
                                if (soloParticipant != null) {
                                    add(
                                        DangerAction(
                                            text = if (soloParticipant.isBlocked) R.string.unblock else R.string.block,
                                            icon = R.drawable.block,
                                            onClick = { showBlockDialog = true }
                                        )
                                    )
                                }
                                add(
                                    DangerAction(
                                        text = R.string.delete_conversation,
                                        icon = R.drawable.delete,
                                        onClick = { showDeleteDialog = true }
                                    )
                                )
                            }
                            dangerActions.fastForEachIndexed { index, action ->
                                CuteListItem(
                                    onClick = action.onClick,
                                    shape = CuteListItemDefaults.getItemShape(index, dangerActions.size),
                                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(action.icon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }
                                ) {
                                    Text(
                                        text = stringResource(action.text),
                                        style = MaterialTheme.typography.bodyLargeEmphasized,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(Modifier.height(15.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = seamY - iconSize / 2)
                    ) {
                        if (state.participants.isGroupChat()) {
                            DefaultGroupChatIcon(
                                size = iconSize,
                                shape = MaterialShapes.Cookie9Sided.toShape()
                            )
                        } else {
                            DefaultContactIcon(
                                firstLetter = name.firstOrNull(),
                                contactPfp = state.participants.firstOrNull()?.photoUri,
                                size = iconSize,
                                shape = MaterialShapes.Cookie9Sided.toShape()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutConversationActionsRow(
    threadId: Long,
    participant: Participant,
    onNavigate: (Screen) -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
) {
    val actions = listOf(
        QuickAction(
            icon = R.drawable.call,
            description = R.string.call,
            onClick = { onHandleConversationActions(ConversationActions.CallNumber(participant.rawNumber)) }
        ),
        QuickAction(
            icon = if (participant.contactId != null) R.drawable.contact_filled else R.drawable.add_contact_filled,
            description = if (participant.contactId != null) R.string.view_contact else R.string.add_contact,
            onClick = { onNavigate(participant.contactScreen()) }
        ),
        QuickAction(
            icon = R.drawable.palette_filled,
            description = R.string.customize,
            onClick = { onNavigate(Screen.ConversationTheming(threadId)) }
        )
    )
    val interactionSources = List(actions.size) { remember { MutableInteractionSource() } }

    ButtonGroup(
        overflowIndicator = {}
    ) {
        actions.fastForEachIndexed { index, action ->
            customItem(
                {
                    FilledTonalIconButton(
                        onClick = action.onClick,
                        interactionSource = interactionSources[index],
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .weight(1f)
                            .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                            .animateWidth(interactionSources[index])
                    ) {
                        Icon(
                            painter = painterResource(action.icon),
                            contentDescription = stringResource(action.description)
                        )
                    }
                },
                {}
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun Participant.contactScreen(): Screen =
    contactId?.let { Screen.ContactDetails(it) }
        ?: Screen.ContactEditor(rawContactId = null, prefilledNumber = rawNumber)

private data class QuickAction(
    @DrawableRes val icon: Int,
    @StringRes val description: Int,
    val onClick: () -> Unit
)

private data class DangerAction(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit
)
