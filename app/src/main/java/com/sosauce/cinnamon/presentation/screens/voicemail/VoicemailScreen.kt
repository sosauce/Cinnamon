@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.voicemail

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.domain.model.CuteVoicemail
import com.sosauce.cinnamon.presentation.navigation.Screen
import com.sosauce.cinnamon.presentation.shared_components.NoXFound
import com.sosauce.cinnamon.presentation.shared_components.SelectedBarSurface
import com.sosauce.cinnamon.presentation.shared_components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.presentation.shared_components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import com.sosauce.sweetselect.rememberSweetSelectState

@Composable
fun VoicemailScreen(
    state: VoicemailState,
    onNavigateUp: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onDeleteVoicemails: (List<Long>) -> Unit
) {

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val exoPlayer = retain {
        ExoPlayer
            .Builder(context.applicationContext)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }
    var isVoicemailPlaying by remember { mutableStateOf(false) }
    var activeVoicemail by remember { mutableStateOf<CuteVoicemail?>(null) }
    val sweetSelectState = rememberSweetSelectState<CuteVoicemail>()



    RetainedEffect(exoPlayer) {

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                isVoicemailPlaying = isPlaying
            }

        }
        exoPlayer.addListener(listener)

        onRetire {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                AnimatedContent(
                    targetState = sweetSelectState.isInSelectionMode,
                ) {
                    if (it) {
                        SelectedBarSurface(
                            modifier = Modifier.selfAlignHorizontally(),
                            items = state.voicemails,
                            multiSelectState = sweetSelectState
                        ) {
                            Button(
                                onClick = {
                                    val ids = sweetSelectState.selectedItems.map { it.id }
                                    onDeleteVoicemails(ids)
                                    sweetSelectState.clearSelected()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        CuteSearchbar(
                            modifier = Modifier.selfAlignHorizontally(),
                            textFieldState = state.textFieldState,
                            navigationIcon = {
                                CuteNavigationButton(
                                    onNavigateUp = onNavigateUp
                                )
                            },
                            onNavigate = onNavigate
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                state = listState
            ) {
                if (state.voicemails.isNotEmpty()) {
                    items(
                        items = state.voicemails,
                        key = { it.id }
                    ) { voicemail ->

                        val isSelected by sweetSelectState.isSelectedAsState(voicemail)

                        VoicemailItem(
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (sweetSelectState.isInSelectionMode) {
                                    sweetSelectState.toggle(voicemail)
                                }
                            },
                            onLongClick = { sweetSelectState.toggle(voicemail) },
                            voicemail = voicemail,
                            isSelected = isSelected,
                            isPlaying = activeVoicemail == voicemail && isVoicemailPlaying,
                            onPlayPause = {
                                if (activeVoicemail != voicemail) {
                                    activeVoicemail = voicemail

                                    exoPlayer.setMediaItem(MediaItem.fromUri(voicemail.uri))
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                } else {
                                    if (isVoicemailPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                }
                            },
                            onNavigate = onNavigate,
                            onDelete = { onDeleteVoicemails(listOf(voicemail.id)) }
                        )
                    }
                } else {
                    item {
                        NoXFound(
                            headlineText = R.string.no_voicemail_found,
                            bodyText = R.string.no_voicemail_found_desc,
                            icon = R.drawable.voicemail
                        )
                    }
                }
            }
        }
    }

}