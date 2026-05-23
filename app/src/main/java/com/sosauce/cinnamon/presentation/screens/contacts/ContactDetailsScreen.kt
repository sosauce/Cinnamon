@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.screens.contacts.components.ContactActionsRow
import com.sosauce.cinnamon.presentation.screens.contacts.components.ContactInfos
import com.sosauce.cinnamon.presentation.screens.messages.components.bottombar.MoreOptions
import com.sosauce.cinnamon.presentation.screens.phone.CallAction
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.animations.AnimatedMoreIcon
import com.sosauce.cinnamon.presentation.shared_components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.utils.SharedTransitionKeys
import com.sosauce.cinnamon.utils.getItemShape

@Composable
fun SharedTransitionScope.ContactDetailsScreen(
    state: ContactDetailsState,
    onNavigateBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onHandleCallAction: (CallAction) -> Unit,
    onHandleContactDetailsAction: (ContactDetailsAction) -> Unit
) {

    var showMoreOptions by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var playFavoriteAnimation by remember { mutableStateOf(false) }

    val moreOptions = listOf(
//        MoreOptions(
//            onClick = {},
//            icon = R.drawable.share_outlined,
//            text = R.string.share
//        ),
        MoreOptions(
            onClick = { showBlockDialog = true },
            icon = R.drawable.block,
            text = R.string.block,
            tint = MaterialTheme.colorScheme.error
        ),
        MoreOptions(
            onClick = {
                onHandleContactDetailsAction(ContactDetailsAction.DeleteContact)
                onNavigateBack()
            },
            icon = R.drawable.delete,
            text = R.string.delete,
            tint = MaterialTheme.colorScheme.error
        )
    )


    if (showBlockDialog) {

        var blockEmailsToo by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.block),
                    contentDescription = null
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlockDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHandleContactDetailsAction(
                            ContactDetailsAction.BlockContact(
                                blockEmailsToo
                            )
                        )
                        showBlockDialog = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.block))
                }
            },
            text = {
                Column {
                    Text(stringResource(R.string.block_contact_u_sure, state.contact.displayName))

                    val corners by animateDpAsState(
                        if (blockEmailsToo) 50.dp else 12.dp
                    )
                    val color by animateColorAsState(
                        if (blockEmailsToo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
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
                        Text(
                            text = stringResource(id = R.string.block_emails)
                        )
                    }
                }
            }
        )
    }


    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {

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


        Scaffold(
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CuteNavigationButtonSurface(onNavigateUp = onNavigateBack)
                    SmallFloatingActionButton(
                        onClick = {},
                        shape = RoundedCornerShape(14.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        content = {
                            Row {
                                IconButton(
                                    onClick = { onNavigate(Screen.ContactEditor(state.contact)) },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.edit_filled),
                                        contentDescription = null
                                    )
                                }
                                IconButton(
                                    onClick = { showMoreOptions = true },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    AnimatedMoreIcon(showMoreOptions)
                                }
                                DropdownMenuPopup(
                                    expanded = showMoreOptions,
                                    onDismissRequest = { showMoreOptions = false }
                                ) {
                                    DropdownMenuGroup(
                                        shapes = MenuDefaults.groupShapes()
                                    ) {
                                        moreOptions.fastForEachIndexed { index, option ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    option.onClick()
                                                    showMoreOptions = false
                                                },
                                                shape = MenuDefaults.getItemShape(
                                                    index,
                                                    moreOptions.lastIndex
                                                ),
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(option.icon),
                                                        contentDescription = null,
                                                        tint = option.tint
                                                            ?: LocalContentColor.current
                                                    )
                                                },
                                                text = {
                                                    Text(
                                                        text = stringResource(option.text),
                                                        color = option.tint
                                                            ?: LocalContentColor.current
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        ) { pv ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(pv)
                    .padding(horizontal = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(200.dp)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_POSTER),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                            .clip(RoundedCornerShape(24.dp))
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
                                .cloudy(30),
                            contentScale = ContentScale.Crop
                        )
                    }
                    DefaultContactIcon(
                        firstLetter = state.contact.displayName.firstOrNull(),
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_PFP),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            ),
                        size = 170.dp,
                        contactPfp = state.contact.photo,
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    )

                }
                Spacer(Modifier.height(15.dp))
                Text(
                    text = state.contact.displayName,
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_NAME + state.contact.id),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current
                        )
                        .basicMarquee(),
                    style = MaterialTheme.typography.headlineLargeEmphasized
                )
                if (state.contact.details.company.isNotEmpty()) {
                    Text(
                        text = state.contact.details.company,
                        style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(Modifier.height(15.dp))
                ContactActionsRow(
                    contact = state.contact,
                    onNavigate = onNavigate,
                    onHandleCallAction = onHandleCallAction,
                    onHandleContactDetailsAction = onHandleContactDetailsAction,
                    onPlayFavoriteAnimation = { playFavoriteAnimation = true }
                )
                Spacer(Modifier.height(25.dp))
                ContactInfos(
                    contact = state.contact,
                    onHandleCallAction = onHandleCallAction,
                    onNavigate = onNavigate
                )

            }
        }

    }


}
