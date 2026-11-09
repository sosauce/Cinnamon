@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.setup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.sosauce.cinnamon.core.datastore.rememberIsLandscape
import com.sosauce.cinnamon.core.ui.components.CuteHero

@Composable
fun SetupStep(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    shape: RoundedPolygon,
    buttonText: String,
    onClick: () -> Unit
) {
    val isLandscape = rememberIsLandscape()

    if (isLandscape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                CuteHero(
                    icon = icon,
                    title = title,
                    description = description,
                    shape = shape,
                    size = 140.dp
                )
            }
            SetupButton(
                icon = icon,
                text = buttonText,
                onClick = onClick,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CuteHero(
                    icon = icon,
                    title = title,
                    description = description,
                    shape = shape
                )
            }
            SetupButton(
                icon = icon,
                text = buttonText,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun SetupButton(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            .height(ButtonDefaults.MediumContainerHeight)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.MediumIconSize)
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMediumEmphasized
        )
    }
}
