@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.phone.presentation.call.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

@Composable
fun Dialpad(
    onSendTone: (Char) -> Unit
) {

    var value by retain { mutableStateOf("") }
    val row1 = listOf('1', '2', '3')
    val row2 = listOf('4', '5', '6')
    val row3 = listOf('7', '8', '9')
    val row4 = listOf('*', '0', '#')
    val scrollState = rememberScrollState()
    LaunchedEffect(value) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expressive display — tonal pill with emphasis
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Enter number",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = value,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmallEmphasized.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Expressive key grid — 8dp spacing system, tonal Squircle buttons
        val all = listOf(row1, row2, row3, row4)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            all.fastForEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.fastForEach { number ->
                        val isSpecial = number == '*' || number == '#'
                        FilledTonalButton(
                            onClick = {
                                value += number
                                onSendTone(number)
                            },
                            shapes = ButtonDefaults.shapes(
                                shape = RoundedCornerShape(20.dp),
                                pressedShape = RoundedCornerShape(14.dp)
                            ),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSpecial) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.filledTonalButtonElevation(
                                defaultElevation = 1.dp,
                                pressedElevation = 3.dp
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            contentPadding = ButtonDefaults.ContentPadding
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = number.toString(),
                                    style = MaterialTheme.typography.headlineSmallEmphasized.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                                // Subtle T9 letters for expressive detail
                                val sub = when (number) {
                                    '2' -> "ABC"
                                    '3' -> "DEF"
                                    '4' -> "GHI"
                                    '5' -> "JKL"
                                    '6' -> "MNO"
                                    '7' -> "PQRS"
                                    '8' -> "TUV"
                                    '9' -> "WXYZ"
                                    else -> null
                                }
                                if (sub != null) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
