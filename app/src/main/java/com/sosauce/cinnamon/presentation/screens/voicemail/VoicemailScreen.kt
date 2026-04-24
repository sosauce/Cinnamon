@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.voicemail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sosauce.cinnamon.data.managers.AudioManager
import com.sosauce.cinnamon.presentation.shared_components.searchbars.MiniCuteSearchbar

@Composable
fun VoicemailScreen(
    state: VoicemailState,
    onNavigateUp: () -> Unit
) {

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContainedLoadingIndicator()
        }
    } else {
        val context = LocalContext.current
        val listState = rememberLazyListState()
        val textState = rememberTextFieldState()

        DisposableEffect(Unit) {
            AudioManager.initializePlayer(context.applicationContext)
            onDispose {
                AudioManager.releasePlayer()
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                MiniCuteSearchbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
                        .navigationBarsPadding(),
                    textFieldState = textState,
                    onNavigateUp = onNavigateUp
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                state = listState
            ) {
                items(
                    items = state.voicemails,
                    key = { it.id }
                ) { voicemail ->
                    VoicemailItem(voicemail)
                }
            }
        }
    }

}