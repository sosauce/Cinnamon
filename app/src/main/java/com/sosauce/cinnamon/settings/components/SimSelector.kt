@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components

import android.telecom.PhoneAccount
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard

@Composable
fun SimSelector(
    simCard: CuteSimCard,
    onClick: () -> Unit,
    isDefaultSim: Boolean
) {


    val borderColor by animateColorAsState(
        targetValue = if (isDefaultSim) Color(simCard.color) else Color.Transparent,
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(10.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .size(50.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = MaterialShapes.Cookie9Sided.toShape()
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.sim_card_filled),
                contentDescription = null,
                tint = Color(simCard.color)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = simCard.name,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            modifier = Modifier.padding(5.dp)
        )
    }
}

@Composable
fun PhoneAccountHandleSelector(
    account: PhoneAccount,
    onClick: () -> Unit,
    isDefaultHandle: Boolean
) {


    val borderColor by animateColorAsState(
        targetValue = if (isDefaultHandle) Color(account.highlightColor) else Color.Transparent,
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(10.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .size(50.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = MaterialShapes.Cookie9Sided.toShape()
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.sim_card_filled),
                contentDescription = null,
                tint = Color(account.highlightColor)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = account.label?.toString() ?: "<unknown>",
            style = MaterialTheme.typography.bodyMediumEmphasized,
            modifier = Modifier.padding(5.dp)
        )
    }
}