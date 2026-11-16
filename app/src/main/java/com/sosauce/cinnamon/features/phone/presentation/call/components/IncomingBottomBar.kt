@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.nekobites.animations.bouncySpec

@Composable
fun IncomingBottomBar(
    onCallAction: (CallAction) -> Unit,
    hasOtherCall: Boolean = false // call waiting: offer hold & answer / end & answer
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    onCallAction(CallAction.DeclineCall)
                },
                shapes = ButtonDefaults.shapes(
                    shape = RoundedCornerShape(50),
                    pressedShape = RoundedCornerShape(20.dp)
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.hang_up_filled),
                    contentDescription = stringResource(R.string.decline),
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize)
                )
                Text(
                    text = stringResource(R.string.decline),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onCallAction(if (hasOtherCall) CallAction.HoldAndAnswer else CallAction.AnswerCall)
                },
                shapes = ButtonDefaults.shapes(
                    shape = RoundedCornerShape(50),
                    pressedShape = RoundedCornerShape(20.dp)
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                val answerLabel =
                    stringResource(if (hasOtherCall) R.string.hold_and_answer else R.string.answer)
                Icon(
                    painter = painterResource(if (hasOtherCall) R.drawable.pause_filled else R.drawable.phone_filled),
                    contentDescription = answerLabel,
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize)
                )
                Text(
                    text = answerLabel,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .basicMarquee()
                )
            }
        }

        AnimatedVisibility(
            visible = hasOtherCall,
            enter = expandVertically(bouncySpec()) + fadeIn(),
            exit = shrinkVertically(bouncySpec()) + fadeOut()
        ) {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onCallAction(CallAction.EndAndAnswer)
                },
                shapes = ButtonDefaults.shapes(
                    shape = RoundedCornerShape(50),
                    pressedShape = RoundedCornerShape(20.dp)
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(64.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.hang_up_filled),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize)
                )
                Text(
                    text = stringResource(R.string.end_and_answer),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .basicMarquee()
                )
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun IncomingBottomBarPreview() {
    CinnamonTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            IncomingBottomBar(onCallAction = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IncomingBottomBarPreviewCallWaiting() {
    CinnamonTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            IncomingBottomBar(onCallAction = {}, hasOtherCall = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IncomingBottomBarPreviewLight() {
    CinnamonTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            IncomingBottomBar(onCallAction = {})
        }
    }
}
