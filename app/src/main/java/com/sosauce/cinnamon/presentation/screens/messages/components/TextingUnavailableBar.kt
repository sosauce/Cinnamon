@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.messages.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.utils.ICON_TEXT_SPACING

@Composable
fun TextingUnavailableBar(
    reason: TextingUnavailableReason
) {

    val reasonText = when (reason) {
        TextingUnavailableReason.AIRPLANE_MODE_ON -> R.string.cant_text_while_airplane
        TextingUnavailableReason.SHORT_CODE -> R.string.reply_short_code_unavailable
    }

    val reasonIcon = when (reason) {
        TextingUnavailableReason.AIRPLANE_MODE_ON -> R.drawable.airplane_mode
        TextingUnavailableReason.SHORT_CODE -> R.drawable.block
    }

    HorizontalFloatingToolbar(
        expanded = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .navigationBarsPadding()
    ) {
        Icon(
            painter = painterResource(reasonIcon),
            contentDescription = null
        )
        Spacer(Modifier.width(ICON_TEXT_SPACING.dp))
        Text(stringResource(reasonText))
    }
}


enum class TextingUnavailableReason {
    SHORT_CODE,
    AIRPLANE_MODE_ON
}