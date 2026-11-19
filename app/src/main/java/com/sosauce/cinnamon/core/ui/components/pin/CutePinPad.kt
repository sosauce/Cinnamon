@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui.components.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sosauce.cinnamon.R
import com.sosauce.nekobites.animations.rememberAnimatedShape
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun CutePinDots(
    length: Int,
    maxLength: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.animateContentSize()
    ) {
        repeat(maxLength) { index ->
            AnimatedVisibility(
                visible = index < length,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialShapes.Cookie4Sided.toShape())
                )
            }
        }
    }
}

/**
 * Numeric keypad used to type a PIN: digits, delete and confirm.
 */
@Composable
fun CutePinPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canDelete: Boolean = true,
    canConfirm: Boolean = true
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .widthIn(max = 400.dp)
            .fillMaxWidth()
    ) {
        rows.fastForEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.fastForEach { digit ->
                    PinKey(
                        onClick = { onDigit(digit) },
                        enabled = enabled
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.headlineSmallEmphasized
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PinKey(
                onClick = onDelete,
                enabled = enabled && canDelete
            ) {
                Icon(
                    painter = painterResource(R.drawable.backspace),
                    contentDescription = stringResource(R.string.delete_digit)
                )
            }
            PinKey(
                onClick = { onDigit('0') },
                enabled = enabled
            ) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )
            }
            PinKey(
                onClick = onConfirm,
                enabled = enabled && canConfirm,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = stringResource(R.string.confirm)
                )
            }
        }
    }
}

@Composable
private fun RowScope.PinKey(
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor)
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
    ) {
        content()
    }
}
