@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings.components


import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import com.sosauce.cinnamon.core.datastore.rememberAppTheme
import com.sosauce.cinnamon.core.utils.CuteTheme
import com.sosauce.cinnamon.core.utils.toPaletteStyle
import com.sosauce.nekobites.components.Spacer

@Composable
fun PaletteSelector(
    isSelected: Boolean,
    paletteStyle: String,
    onSelectNewPalette: () -> Unit
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val theme by rememberAppTheme()
    val isDark = when (theme) {
        CuteTheme.DARK, CuteTheme.AMOLED -> true
        CuteTheme.SYSTEM -> isSystemInDarkTheme
        else -> false
    }

    val state = rememberDynamicMaterialThemeState(
        seedColor = MaterialTheme.colorScheme.primary,
        isDark = isDark,
        isAmoled = theme == CuteTheme.AMOLED,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = paletteStyle.toPaletteStyle()
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
    )

    DynamicMaterialExpressiveTheme(
        state = state,
        animate = true
    ) {
        val dynamicColors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onSelectNewPalette)
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .width(60.dp)
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                dynamicColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            }

            Spacer(10.dp)
            Text(
                text = paletteStyle,
                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                    color = textColor
                )
            )
        }
    }
}