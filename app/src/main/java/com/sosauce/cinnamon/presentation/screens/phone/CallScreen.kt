package com.sosauce.cinnamon.presentation.screens.phone

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.fetchers.PhotoQuality
import com.sosauce.cinnamon.domain.model.AudioRoute
import com.sosauce.cinnamon.domain.states.CallState
import com.sosauce.cinnamon.presentation.screens.phone.components.CallBottomBar
import com.sosauce.cinnamon.presentation.screens.phone.components.IncomingBottomBar
import com.sosauce.cinnamon.presentation.shared_components.DefaultContactIcon
import com.sosauce.cinnamon.presentation.theme.CinnamonTheme
import com.sosauce.cinnamon.utils.bouncySpec
import com.sosauce.cinnamon.utils.toStopwatch
import kotlin.time.DurationUnit

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallScreen(
    onCallAction: (CallAction) -> Unit,
    callUiState: CallingState
) {

    Scaffold(
        bottomBar = {
            AnimatedContent(
                targetState = callUiState.callState == CallState.RINGING,
                transitionSpec = { scaleIn(bouncySpec()) + fadeIn() togetherWith scaleOut(bouncySpec()) + fadeOut() }
            ) {
                if (it) {
                    IncomingBottomBar(
                        onCallAction = onCallAction
                    )
                } else {
                    CallBottomBar(
                        onCallAction = onCallAction,
                        callUiState = callUiState
                    )
                }
            }
        }
    ) { paddingValues ->
        AsyncImage(
            model = callUiState.poster.toUri(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(
                color = Color.Black.copy(alpha = 0.2f),
                blendMode = BlendMode.Darken
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DefaultContactIcon(
                firstLetter = callUiState.displayName.firstOrNull(),
                size = 250.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                contactPhoneNumber = callUiState.number,
                quality = PhotoQuality.FULL_QUALITY
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = callUiState.displayName,
                maxLines = 1,
                style = MaterialTheme.typography.displaySmallEmphasized.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.basicMarquee()
            )
            val secondaryText = when (callUiState.callState) {
                CallState.RINGING -> buildAnnotatedString {
                    append(stringResource(R.string.via))
                    append(" ")
                    withStyle(SpanStyle(color = Color(callUiState.activeSim.color))) {
                        append(callUiState.activeSim.name)
                    }
                }

                CallState.DIALING -> AnnotatedString(stringResource(R.string.ringing))
                CallState.ENDED -> AnnotatedString(stringResource(R.string.call_ended))
                CallState.ONGOING -> AnnotatedString(
                    callUiState.timeSpentInCall.toStopwatch(
                        DurationUnit.SECONDS
                    )
                )
            }
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            AnimatedVisibility(
                visible = callUiState.isHolding,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Row {
                    Icon(
                        painter = painterResource(R.drawable.pause_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.on_hold),
                        style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                            MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CallScreenPreview() {
    CinnamonTheme {
        CallScreen(
            onCallAction = {},
            callUiState = CallingState(
                number = "sosauce",
                displayName = "sosauce",
                callState = CallState.RINGING,
                availableAudioRoutes = listOf(
                    AudioRoute(name = "Speaker"),
                    AudioRoute(name = "Name")
                )
            )
        )
    }
}

