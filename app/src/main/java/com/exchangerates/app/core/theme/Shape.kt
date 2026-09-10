package com.exchangerates.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object AppDimens {
    val screenPadding = 16.dp
    val cardCorner = 22.dp
    val cardPaddingH = 20.dp
    val cardPaddingV = 18.dp
    val cardGap = 10.dp
    val flagSize = 30.dp
    val operatorButton = 56.dp
}
