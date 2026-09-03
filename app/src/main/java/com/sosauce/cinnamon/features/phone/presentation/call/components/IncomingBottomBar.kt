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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction

/**
 * M3 Expressive incoming call bottom bar — no swipe, only Answer / Decline
 * Full pill shapes, tonal error/primary containers, spring motion.
 */
@Composable
fun IncomingBottomBar(
    onCallAction: (CallAction) -> Unit
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
        // Expressive hint — M3 LabelLargeEmphasized with tonal color
        Text(
            text = "Incoming call",
            style = MaterialTheme.typography.labelLargeEmphasized.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Expressive button row — 8dp system, tonal, elevated
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Decline — error container, full pill, icon rotated 135°
            androidx.compose.material3.Button(
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
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.phone_filled),
                    contentDescription = "Decline",
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = 135f }
                )
                Text(
                    text = "Decline",
                    style = MaterialTheme.typography.titleSmallEmphasized.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            // Answer — primary container, full pill
            androidx.compose.material3.Button(
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
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.phone_filled),
                    contentDescription = "Answer",
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Answer",
                    style = MaterialTheme.typography.titleSmallEmphasized.copy(fontWeight = FontWeight.ExtraBold),
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
