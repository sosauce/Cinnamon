@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.about

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.AttachmentType
import com.sosauce.cinnamon.presentation.screens.messages.ConversationActions
import com.sosauce.cinnamon.presentation.screens.messages.ConversationDetailsState
import com.sosauce.cinnamon.presentation.screens.messages.components.bubble.ImageAttachment
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.shared_components.DefaultGroupChatIcon
import com.sosauce.cinnamon.presentation.shared_components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.utils.SharedTransitionKeys
import com.sosauce.cinnamon.utils.selfAlignHorizontally

@Composable
fun SharedTransitionScope.AboutConversationScreen(
    state: ConversationDetailsState,
    onNavigateBack: () -> Unit,
    onHandleConversationActions: (ConversationActions) -> Unit
) {

    val context = LocalContext.current
    val isGroupChat = state.recipients.size > 1
    val medias = remember(state.messages) {
        state.messages.values.flatten().fastFlatMap { message ->
            message.attachment?.attachmentDetails
                ?.fastFilter {
                    it.attachmentType == AttachmentType.IMAGE ||
                            it.attachmentType == AttachmentType.VIDEO
                } ?: emptyList()
        }
    }

    Scaffold(
        bottomBar = {
            CuteNavigationButton(onNavigateUp = onNavigateBack)
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(SharedTransitionKeys.CONVERSATION_WALLPAPER),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            renderInOverlayDuringTransition = false

                        )
                        .clip(RoundedCornerShape(24.dp))
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
                if (isGroupChat) {
                    DefaultGroupChatIcon(
                        size = 170.dp,
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    )
                } else {
                    DefaultContactIcon(
                        firstLetter = state.nameOrBeautifiedRecipients.firstOrNull()?.firstOrNull(),
                        size = 170.dp,
                        contactPhoneNumber = state.recipients.firstOrNull(),
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            Text(
                text = buildString {
                    state.nameOrBeautifiedRecipients.fastForEachIndexed { index, recipient ->
                        append(recipient)
                        if (index != state.nameOrBeautifiedRecipients.lastIndex) {
                            append(", ")
                        }
                    }
                },
                modifier = Modifier
                    .selfAlignHorizontally()
                    .basicMarquee(),
                style = MaterialTheme.typography.headlineLargeEmphasized
            )


            Spacer(Modifier.height(15.dp))

            if (medias.isNotEmpty()) {
                Text(
                    text = "${
                        pluralStringResource(
                            id = R.plurals.medias_plural,
                            medias.size
                        )
                    }: ${medias.size}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 34.dp, vertical = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    HorizontalMultiBrowseCarousel(
                        state = rememberCarouselState { medias.count() },
                        preferredItemWidth = 186.dp,
                        itemSpacing = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 16.dp, bottom = 16.dp),
                    ) { index ->
                        val media = medias[index]
                        ImageAttachment(
                            image = media.uri,
                            modifier = Modifier
                                .height(205.dp)
                                .maskClip(MaterialTheme.shapes.extraLarge),
                            onHandleConversationActions = onHandleConversationActions
                        )

                    }
                }
            }


        }
    }
}