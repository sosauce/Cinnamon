@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.contacts.presentation

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
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
import com.skydoves.cloudy.liquidGlass
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.DefaultContactIcon
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.core.utils.SharedTransitionKeys
import com.sosauce.cinnamon.core.utils.getItemShape
import com.sosauce.cinnamon.features.contacts.presentation.components.ContactActionsRow
import com.sosauce.cinnamon.features.contacts.presentation.components.ContactInfos
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.MoreOptions
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.components.LoadingBox

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
    val cookie9Sided = MaterialShapes.Cookie9Sided.toShape()

    val moreOptions = listOf(
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
                    Surface(
                        shadowElevation = 5.dp,
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    ) {
                        CuteNavigationButtonSurface(onNavigateUp = onNavigateBack)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 5.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row {
                            IconButton(
                                onClick = {
                                    val contact = state.contact
                                    val details = state.details
                                    onNavigate(
                                        Screen.ContactEditor(
                                            contact = contact,
                                            details = details
                                        )
                                    )
                                },
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
                                AnimatedDrawable(
                                    atEnd = showMoreOptions,
                                    drawable = AnimatedDrawableFile.MORE_VERT
                                )
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
                }
            }
        ) { pv ->
            // Header metrics: poster bleeds under the status bar, the contact icon
            // sits centered on the poster/card seam (clear of the status bar), and
            // the content card overlaps the poster's bottom edge.
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
                            modifier = Modifier.fillMaxSize().cloudy(5),
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
                                        sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_NAME + state.contact.id),
                                        animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                    )
                                    .basicMarquee(),
                                style = MaterialTheme.typography.headlineLargeEmphasized
                            )
                            state.details.company?.let { company ->
                                Text(
                                    text = company,
                                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Spacer(Modifier.height(15.dp))
                            ContactActionsRow(
                                state = state,
                                onNavigate = onNavigate,
                                onHandleCallAction = onHandleCallAction,
                                onHandleContactDetailsAction = onHandleContactDetailsAction,
                                onPlayFavoriteAnimation = { playFavoriteAnimation = true }
                            )
                            Spacer(Modifier.height(25.dp))
                            ContactInfos(
                                state = state,
                                onHandleCallAction = onHandleCallAction,
                                onNavigate = onNavigate
                            )
                        }
                    }
                    val surfaceContainerHighest = MaterialTheme.colorScheme.surfaceContainer
                    DefaultContactIcon(
                        firstLetter = state.contact.displayName.firstOrNull(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = seamY - iconSize / 2)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONTACT_PFP),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                            .drawWithCache {
                                val path = cookie9Sided.createOutline(size, layoutDirection, this)
                                    .let { outline ->
                                        Path().apply {
                                            when (outline) {
                                                is Outline.Generic -> addPath(outline.path)
                                                is Outline.Rounded -> addRoundRect(outline.roundRect)
                                                is Outline.Rectangle -> addRect(outline.rect)
                                            }
                                        }
                                    }
                                onDrawBehind {
                                    withTransform(
                                        {
                                            scale(
                                                scaleX = 1.08f,
                                                scaleY = 1.08f,
                                                pivot = center
                                            )
                                        }
                                    ) {
                                        drawPath(
                                            path = path,
                                            color = surfaceContainerHighest
                                        )
                                    }
                                }
                            },
                        size = iconSize,
                        contactPfp = state.details.photo,
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    )

                }

            }
        }
    }


}
