@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.core.ui.components.dialogs.BlockUnblockDialog
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.items.CuteListItemDefaults
import com.sosauce.cinnamon.core.ui.components.text.HeaderText
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.cinnamon.features.contacts.presentation.components.ContactActionsRow
import com.sosauce.cinnamon.features.contacts.presentation.components.ContactInfos
import com.sosauce.nekobites.components.LoadingBox
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.ContactDetailsScreen(
    state: ContactDetailsState,
    onNavigateBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onHandleContactDetailsAction: (ContactDetailsAction) -> Unit
) {

    var showBlockDialog by remember { mutableStateOf(false) }
    var blockEmailsToo by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var playFavoriteAnimation by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shortcutBackground = MaterialTheme.colorScheme.primary.toArgb()
    val shortcutForeground = MaterialTheme.colorScheme.onPrimary.toArgb()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.delete_contact)) },
            text = { Text(stringResource(R.string.delete_contact_u_sure, state.contact.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHandleContactDetailsAction(ContactDetailsAction.DeleteContact)
                        showDeleteDialog = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAccountPicker) {
        AlertDialog(
            onDismissRequest = { showAccountPicker = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.contact_filled),
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.select_account_to_edit)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showAccountPicker = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                Column {
                    state.rawContacts.fastForEachIndexed { index, rawContact ->
                        CuteListItem(
                            onClick = {
                                showAccountPicker = false
                                onNavigate(
                                    Screen.ContactEditor(
                                        rawContactId = rawContact.id
                                    )
                                )
                            },
                            shape = CuteListItemDefaults.getItemShape(
                                index,
                                state.rawContacts.count()
                            ),
                            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            leadingContent = {
                                Text(
                                    text = "${index + 1}.",
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        ) {
                            Text(if (rawContact.accountName.equals("Device", ignoreCase = true) || rawContact.accountName.isBlank()) stringResource(R.string.device) else rawContact.accountName)
                        }
                    }
                }
            }
        )
    }


    if (showBlockDialog) {
        BlockUnblockDialog(
            displayName = state.contact.displayName,
            isBlocked = false,
            onDismissRequest = {
                showBlockDialog = false
                blockEmailsToo = false
            },
            onConfirm = {
                onHandleContactDetailsAction(
                    ContactDetailsAction.BlockContact(blockEmailsToo)
                )
                showBlockDialog = false
                blockEmailsToo = false
            },
            confirmationText = stringResource(
                R.string.block_contact_u_sure,
                state.contact.displayName
            ),
            additionalContent = {
                val corners by animateDpAsState(if (blockEmailsToo) 50.dp else 12.dp)
                val color by animateColorAsState(
                    if (blockEmailsToo) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                )

                Button(
                    onClick = { blockEmailsToo = !blockEmailsToo },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(corners),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color,
                        contentColor = contentColorFor(color)
                    )
                ) {
                    AnimatedVisibility(
                        visible = blockEmailsToo,
                        modifier = Modifier.padding(end = 5.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null
                        )
                    }
                    Text(stringResource(R.string.block_emails))
                }
            }
        )
    }
    AnimatedVisibility(
        visible = playFavoriteAnimation,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.zIndex(999f)
    ) {
        val raw = if (state.contact.isFavorite) R.raw.heart else R.raw.broken_heart

        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(raw)
        )
        val progress by animateLottieCompositionAsState(
            composition = composition
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(120.dp)
            )
        }

        LaunchedEffect(progress) {
            if (progress == 1f) playFavoriteAnimation = false
        }
    }


    LoadingBox(
        isLoading = state.isLoading
    ) {
        Scaffold(
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CuteNavigationButtonSurface(onNavigateUp = onNavigateBack)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row {
                            IconButton(
                                onClick = {
                                    if (state.rawContacts.size > 1) {
                                        showAccountPicker = true
                                    } else {
                                        onNavigate(
                                            Screen.ContactEditor(
                                                rawContactId = state.rawContacts.firstOrNull()?.id
                                            )
                                        )
                                    }
                                },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.edit_filled),
                                    contentDescription = stringResource(R.string.edit_contact)
                                )
                            }
                        }
                    }
                }
            }
        ) { pv ->
            val posterHeight = 280.dp
            val iconSize = 170.dp
            val cardOverlap = 25.dp
            val seamY = posterHeight - cardOverlap
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(PaddingValues(bottom = pv.calculateBottomPadding()))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(posterHeight + 20.dp) // keep the 20.dp pls
                            .align(Alignment.TopCenter)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_POSTER),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        )
                        AsyncImage(
                            model = state.settings.poster,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .cloudy(5),
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
                                text = state.contact.displayName,
                                modifier = Modifier
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(
                                            SharedTransitionKeys.CONTACT_NAME + state.contact.id
                                        ),
                                        animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                    )
                                    .basicMarquee(),
                                style = MaterialTheme.typography.headlineLargeEmphasized
                            )
                            if (state.details.company != null || state.labels.isNotEmpty()) {
                                Spacer(Modifier.height(5.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    itemVerticalAlignment = Alignment.CenterVertically
                                ) {
                                    state.details.company?.let { company ->
                                        DetailsChip(
                                            icon = R.drawable.company,
                                            text = company
                                        )
                                    }
                                    state.labels.forEach { label ->
                                        DetailsChip(
                                            icon = R.drawable.label,
                                            text = label
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(15.dp))
                            ContactActionsRow(
                                state = state,
                                onNavigate = onNavigate,
                                onHandleContactDetailsAction = onHandleContactDetailsAction,
                                onPlayFavoriteAnimation = { playFavoriteAnimation = true }
                            )
                            Spacer(Modifier.height(25.dp))
                            ContactInfos(
                                state = state,
                                onHandleContactDetailsAction = onHandleContactDetailsAction,
                                onNavigate = onNavigate
                            )
                            Spacer(Modifier.height(16.dp))
                            Column {
                                HeaderText(stringResource(R.string.contact_settings))
                                ContactSettingItem(
                                    icon = R.drawable.add_to_home_screen,
                                    text = R.string.add_to_home_screen,
                                    shape = CuteListItemDefaults.getItemShape(0, 4),
                                    onClick = {
                                        scope.launch {
                                            val pinned = pinContactShortcut(
                                                context = context,
                                                contact = state.contact,
                                                photo = state.details.photo,
                                                background = shortcutBackground,
                                                foreground = shortcutForeground
                                            )
                                            if (!pinned) {
                                                Toast.makeText(context, R.string.pin_shortcut_unsupported, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                                CuteListItem(
                                    onClick = {
                                        onHandleContactDetailsAction(
                                            ContactDetailsAction.SetSendToVoicemail(!state.details.sendToVoicemail)
                                        )
                                    },
                                    shape = CuteListItemDefaults.getItemShape(1, 4),
                                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.voicemail),
                                            contentDescription = null,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    },
                                    trailingContent = {
                                        Switch(
                                            checked = state.details.sendToVoicemail,
                                            onCheckedChange = {
                                                onHandleContactDetailsAction(ContactDetailsAction.SetSendToVoicemail(it))
                                            },
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                    }
                                ) {
                                    Text(stringResource(R.string.send_to_voicemail))
                                }
                                ContactSettingItem(
                                    icon = R.drawable.block,
                                    text = R.string.block,
                                    shape = CuteListItemDefaults.getItemShape(2, 4),
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { showBlockDialog = true }
                                )
                                ContactSettingItem(
                                    icon = R.drawable.delete,
                                    text = R.string.delete,
                                    shape = CuteListItemDefaults.getItemShape(3, 4),
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = { showDeleteDialog = true }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = seamY - iconSize / 2)
                    ) {
                        DefaultContactIcon(
                            firstLetter = state.contact.displayName.firstOrNull(),
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(
                                        SharedTransitionKeys.CONTACT_PFP
                                    ),
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                ),
                            size = iconSize,
                            contactPfp = state.details.photo,
                            shape = MaterialShapes.Cookie9Sided.toShape()
                        )

                    }

                }

            }
        }
    }


}

@Composable
private fun ContactSettingItem(
    @DrawableRes icon: Int,
    @StringRes text: Int,
    shape: Shape,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    CuteListItem(
        onClick = onClick,
        shape = shape,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    ) {
        Text(
            text = stringResource(text),
            color = tint
        )
    }
}

@Composable
private fun DetailsChip(
    @DrawableRes icon: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                color = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
    }
}
