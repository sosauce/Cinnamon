@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.conversation.components

import androidx.compose.ui.Alignment
import androidx.compose.material3.toShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.ICON_TEXT_SPACING
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally

@Composable
fun TextingUnavailableBar(
    reason: TextingUnavailableReason
) {

    val reasonText = when (reason) {
        TextingUnavailableReason.AIRPLANE_MODE_ON -> R.string.cant_text_while_airplane
        TextingUnavailableReason.SHORT_CODE -> R.string.reply_short_code_unavailable
        TextingUnavailableReason.BLOCKED -> R.string.unblock_them_first
    }

    val reasonIcon = when (reason) {
        TextingUnavailableReason.AIRPLANE_MODE_ON -> R.drawable.airplane_mode
        TextingUnavailableReason.SHORT_CODE, TextingUnavailableReason.BLOCKED -> R.drawable.block
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .navigationBarsPadding()
            .selfAlignHorizontally()
            .fillMaxWidth(0.95f)
            .padding(bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialShapes.Cookie9Sided.toShape())
            ) {
                Icon(
                    painter = painterResource(reasonIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(reasonText),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}


enum class TextingUnavailableReason {
    SHORT_CODE,
    AIRPLANE_MODE_ON,
    BLOCKED
}