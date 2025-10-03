package com.sosauce.cuteconnect.ui.screens.voicemail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sosauce.cuteconnect.data.managers.AudioManager
import com.sosauce.cuteconnect.domain.model.CuteVoicemail
import com.sosauce.cuteconnect.ui.shared_components.searchbars.MiniCuteSearchbar
import com.sosauce.cuteconnect.utils.rememberSearchbarAlignment
import com.sosauce.cuteconnect.utils.rememberSearchbarMaxFloatValue
import com.sosauce.cuteconnect.utils.rememberSearchbarRightPadding
import com.sosauce.cuteconnect.utils.showCuteSearchbar

@Composable
fun VoicemailScreen(
    voicemails: List<CuteVoicemail>,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val textState = rememberTextFieldState()

    DisposableEffect(Unit) {
        AudioManager.initializePlayer(context.applicationContext)
        onDispose {
            AudioManager.releasePlayer()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                state = listState
            ) {
                items(
                    items = voicemails,
                    key = { it.id }
                ) { voicemail ->
                    VoicemailItem(voicemail)

                }
            }
        }

        AnimatedVisibility(
            visible = listState.showCuteSearchbar,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(rememberSearchbarAlignment())
        ) {
            MiniCuteSearchbar(
                textFieldState = textState,
                onNavigateUp = onNavigateUp
            )
        }

    }

}