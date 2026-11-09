package com.sosauce.cinnamon.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GroupedShapeDefaults {
    val Outer = 24.dp
    val Inner = 2.dp
}

fun groupedShape(
    top: Dp,
    bottom: Dp
): Shape = RoundedCornerShape(
    topStart = top,
    topEnd = top,
    bottomStart = bottom,
    bottomEnd = bottom
)

fun groupedShape(
    isFirst: Boolean,
    isLast: Boolean
): Shape = groupedShape(
    top = if (isFirst) GroupedShapeDefaults.Outer else GroupedShapeDefaults.Inner,
    bottom = if (isLast) GroupedShapeDefaults.Outer else GroupedShapeDefaults.Inner
)
