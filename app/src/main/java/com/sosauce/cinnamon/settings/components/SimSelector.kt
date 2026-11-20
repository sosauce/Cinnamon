package com.sosauce.cinnamon.settings.components

import android.telecom.PhoneAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.phone.domain.CuteSimCard
import com.sosauce.nekobites.components.settings.SelectableTile

@Composable
fun SimSelector(
    simCard: CuteSimCard,
    onClick: () -> Unit,
    isDefaultSim: Boolean
) {
    SimTile(
        label = simCard.name,
        color = simCard.color?.let { Color(it) },
        selected = isDefaultSim,
        onClick = onClick
    )
}

@Composable
fun PhoneAccountHandleSelector(
    account: PhoneAccount,
    onClick: () -> Unit,
    isDefaultHandle: Boolean
) {
    SimTile(
        label = account.label?.toString() ?: stringResource(R.string.unknown),
        color = Color(account.highlightColor),
        selected = isDefaultHandle,
        onClick = onClick
    )
}

@Composable
fun AskEveryTimeSelector(
    onClick: () -> Unit,
    isSelected: Boolean
) {
    SelectableTile(
        selected = isSelected,
        onClick = onClick,
        label = stringResource(R.string.ask_every_time)
    ) {
        Icon(
            painter = painterResource(R.drawable.question_mark),
            contentDescription = null
        )
    }
}

@Composable
private fun SimTile(
    label: String,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectableTile(
        selected = selected,
        onClick = onClick,
        label = label
    ) {
        Icon(
            painter = painterResource(R.drawable.sim_card_filled),
            contentDescription = null,
            tint = color ?: LocalContentColor.current
        )
    }
}
