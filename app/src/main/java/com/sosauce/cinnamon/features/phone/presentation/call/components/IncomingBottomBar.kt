@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction

@Composable
fun IncomingBottomBar(
    onCallAction: (CallAction) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val transition = rememberInfiniteTransition()
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

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
                    onCallAction(CallAction.AnswerCall)
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
                    .weight(1.4f)
                    .height(64.dp)
                    .graphicsLayer {
                        scaleX = breath
                        scaleY = breath
                    }
            ) {
                Icon(
                    painter = painterResource(R.drawable.phone_filled),
                    contentDescription = stringResource(R.string.answer),
                    modifier = Modifier.size(ButtonDefaults.MediumIconSize)
                )
                Text(
                    text = stringResource(R.string.answer),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(start = 10.dp)
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
private fun IncomingBottomBarPreviewLight() {
    CinnamonTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            IncomingBottomBar(onCallAction = {})
        }
    }
}
